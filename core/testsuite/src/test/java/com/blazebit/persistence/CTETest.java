/*
 * Copyright 2015 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blazebit.persistence;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.blazebit.persistence.entity.RecursiveEntity;
import com.blazebit.persistence.entity.TestCTE;
import com.blazebit.persistence.testsuite.base.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.category.NoEclipselink;
import com.blazebit.persistence.testsuite.base.category.NoMySQL;
import com.blazebit.persistence.testsuite.base.category.NoOpenJPA;

/**
 *
 * @author Christian Beikov
 * @since 1.1.0
 */
public class CTETest extends AbstractCoreTest {
    
    @Override
	protected Class<?>[] getEntityClasses() {
		return new Class<?>[] {
			RecursiveEntity.class,
			TestCTE.class
		};
	}

	@Before
    public void setUp(){
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            RecursiveEntity root1 = new RecursiveEntity("root1");
            RecursiveEntity child1_1 = new RecursiveEntity("child1_1", root1);
            RecursiveEntity child1_2 = new RecursiveEntity("child1_2", root1);
            
            RecursiveEntity child1_1_1 = new RecursiveEntity("child1_1_1", child1_1);
            RecursiveEntity child1_2_1 = new RecursiveEntity("child1_2_1", child1_2);

            em.persist(root1);
            em.persist(child1_1);
            em.persist(child1_2);
            em.persist(child1_1_1);
            em.persist(child1_2_1);

            em.flush();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }
	
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testCTE(){
        CriteriaBuilder<TestCTE> cb = cbf.create(em, TestCTE.class, "t").where("t.level").ltExpression("2");
        cb.with(TestCTE.class)
        	.from(RecursiveEntity.class, "e")
        	.bind("id").select("e.id")
        	.bind("name").select("e.name")
        	.bind("level").select("0")
        	.where("e.parent").isNull()
        .end();
        String expected = ""
        		+ "WITH " + TestCTE.class.getSimpleName() + "(id, level, name) AS(\n"
        		+ "SELECT e.id, 0, e.name FROM RecursiveEntity e WHERE e.parent IS NULL"
        		+ "\n)\n"
        		+ "SELECT t FROM " + TestCTE.class.getSimpleName() + " t WHERE t.level < 2";
        
        assertEquals(expected, cb.getQueryString());
        List<TestCTE> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("root1", resultList.get(0).getName());
    }
	
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testRecursiveCTE(){
        CriteriaBuilder<TestCTE> cb = cbf.create(em, TestCTE.class, "t").where("t.level").ltExpression("2");
        cb.withRecursive(TestCTE.class)
        	.from(RecursiveEntity.class, "e")
        	.bind("id").select("e.id")
        	.bind("name").select("e.name")
        	.bind("level").select("0")
        	.where("e.parent").isNull()
        .unionAll()
        	.from(TestCTE.class, "t")
        	.from(RecursiveEntity.class, "e")
        	.bind("id").select("e.id")
        	.bind("name").select("e.name")
        	.bind("level").select("t.level + 1")
        	.where("t.id").eqExpression("e.parent.id")
    	.end();
        String expected = ""
        		+ "WITH RECURSIVE " + TestCTE.class.getSimpleName() + "(id, level, name) AS(\n"
        		+ "SELECT e.id, 0, e.name FROM RecursiveEntity e WHERE e.parent IS NULL"
        		+ "\nUNION ALL\n"
        		+ "SELECT e.id, t.level + 1, e.name FROM " + TestCTE.class.getSimpleName() + " t, RecursiveEntity e WHERE t.id = e.parent.id"
        		+ "\n)\n"
        		+ "SELECT t FROM " + TestCTE.class.getSimpleName() + " t WHERE t.level < 2";
        
        assertEquals(expected, cb.getQueryString());
        List<TestCTE> resultList = cb.getResultList();
        assertEquals(3, resultList.size());
        assertEquals("root1", resultList.get(0).getName());
    }
}