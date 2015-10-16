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

import com.blazebit.persistence.entity.Document;
import com.blazebit.persistence.entity.IdHolderCTE;
import com.blazebit.persistence.entity.IntIdEntity;
import com.blazebit.persistence.entity.Person;
import com.blazebit.persistence.entity.Version;
import com.blazebit.persistence.testsuite.base.category.NoDB2;
import com.blazebit.persistence.testsuite.base.category.NoDatanucleus;
import com.blazebit.persistence.testsuite.base.category.NoEclipselink;
import com.blazebit.persistence.testsuite.base.category.NoFirebird;
import com.blazebit.persistence.testsuite.base.category.NoH2;
import com.blazebit.persistence.testsuite.base.category.NoHibernate42;
import com.blazebit.persistence.testsuite.base.category.NoMySQL;
import com.blazebit.persistence.testsuite.base.category.NoOpenJPA;
import com.blazebit.persistence.testsuite.base.category.NoOracle;
import com.blazebit.persistence.testsuite.base.category.NoSQLite;

/**
 *
 * @author Christian Beikov
 * @since 1.1.0
 */
public class SetOperationTest extends AbstractCoreTest {

    Document doc1;
    Document doc2;
    Document doc3;

    @Override
    protected Class<?>[] getEntityClasses() {
        return new Class<?>[] {
            Document.class,
            Version.class,
            IntIdEntity.class,
            Person.class, 
            IdHolderCTE.class
        };
    }
    
    @Before
    public void setUp() {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            doc1 = new Document("D1");
            doc2 = new Document("D2");
            doc3 = new Document("D3");

            Person o1 = new Person("P1");

            doc1.setOwner(o1);
            doc2.setOwner(o1);
            doc3.setOwner(o1);

            em.persist(o1);

            em.persist(doc1);
            em.persist(doc2);
            em.persist(doc3);

            em.flush();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testPrecedence() {
        FinalSetOperationCriteriaBuilder<String> cb = cbf.create(em, String.class)
                    .from(Document.class, "d1")
                    .select("d1.name")
                    .where("d1.name").eq("D1")
                .intersect()
                    .from(Document.class, "d2")
                    .select("d2.name")
                    .where("d2.name").notEq("D2")
                .except()
                    .from(Document.class, "d3")
                    .select("d3.name")
                    .where("d3.name").eq("D3")
                .endSet();
        String expected = ""
                + "SELECT d1.name FROM Document d1 WHERE d1.name = :param_0\n"
                + "INTERSECT\n"
                + "SELECT d2.name FROM Document d2 WHERE d2.name <> :param_1\n"
                + "EXCEPT\n"
                + "SELECT d3.name FROM Document d3 WHERE d3.name = :param_2";
        
        assertEquals(expected, cb.getQueryString());
        List<String> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0));
    }
	
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testUnionAll() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d1")
                .select("d1")
                .where("d1.name").eq("D1")
            .unionAll()
                .from(Document.class, "d2")
                .select("d2")
                .where("d2.name").eq("D1")
            .endSet();
        String expected = ""
        		+ "SELECT d1 FROM Document d1 WHERE d1.name = :param_0\n"
        		+ "UNION ALL\n"
        		+ "SELECT d2 FROM Document d2 WHERE d2.name = :param_0";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(2, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
        assertEquals("D1", resultList.get(1).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testUnion() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d1")
                .select("d1")
                .where("d1.name").eq("D1")
            .union()
                .from(Document.class, "d2")
                .select("d2")
                .where("d2.name").eq("D1")
            .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name = :param_0\n"
                + "UNION\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name = :param_0";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }


    // NOTE: Currently only PostgreSQL support EXCEPT ALL
    @Test
    @Category({ NoH2.class, NoDB2.class, NoOracle.class, NoSQLite.class, NoFirebird.class, NoMySQL.class, NoHibernate42.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class })
    public void testExceptAll() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d1")
                .select("d1")
                .where("d1.name").notEq("D2")
            .exceptAll()
                .from(Document.class, "d2")
                .select("d2")
                .where("d2.name").notEq("D3")
            .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name <> :param_0\n"
                + "EXCEPT ALL\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name <> :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D3", resultList.get(0).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testExcept() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d1")
                .select("d1")
                .where("d1.name").notEq("D2")
            .except()
                .from(Document.class, "d2")
                .select("d2")
                .where("d2.name").notEq("D3")
            .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name <> :param_0\n"
                + "EXCEPT\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name <> :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D3", resultList.get(0).getName());
    }

    // NOTE: Currently only PostgreSQL support INTERSECT ALL
    @Test
    @Category({ NoH2.class, NoDB2.class, NoOracle.class, NoSQLite.class, NoFirebird.class, NoMySQL.class, NoHibernate42.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class })
    public void testIntersectAll() {
        FinalSetOperationCriteriaBuilder<String> cb = cbf.create(em, String.class)
                .from(Document.class, "d1")
                .select("SUBSTRING(d1.name, 1, 1)")
                .where("d1.name").notEq("D2")
            .intersectAll()
                .from(Document.class, "d2")
                .select("SUBSTRING(d2.name, 1, 1)")
                .where("d2.name").notEq("D3")
            .endSet();
        String expected = ""
                + "SELECT SUBSTRING(d1.name,1,1) FROM Document d1 WHERE d1.name <> :param_0\n"
                + "INTERSECT ALL\n"
                + "SELECT SUBSTRING(d2.name,1,1) FROM Document d2 WHERE d2.name <> :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<String> resultList = cb.getResultList();
        assertEquals(2, resultList.size());
        assertEquals("D", resultList.get(0));
        assertEquals("D", resultList.get(1));
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testIntersect() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d1")
                .select("d1")
                .where("d1.name").notEq("D2")
            .intersect()
                .from(Document.class, "d2")
                .select("d2")
                .where("d2.name").notEq("D3")
            .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name <> :param_0\n"
                + "INTERSECT\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name <> :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
    
    /* Set operation nesting */
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testNestedIntersectWithUnion() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
            .startSet(em, Document.class)
                    .from(Document.class, "d1")
                    .select("d1")
                    .where("d1.name").notEq("D2")
                .intersect()
                    .from(Document.class, "d2")
                    .select("d2")
                    .where("d2.name").notEq("D3")
            .endSet()
            .union()
                .from(Document.class, "d3")
                .select("d3")
                .where("d3.name").eq("D3")
            .endSet();
        String expected = ""
                + "(SELECT d1 FROM Document d1 WHERE d1.name <> :param_0\n"
                + "INTERSECT\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name <> :param_1)\n"
                + "UNION\n"
                + "SELECT d3 FROM Document d3 WHERE d3.name = :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(2, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
        assertEquals("D3", resultList.get(1).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testIntersectWithNestedUnion() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class)
                .from(Document.class, "d1")
                .select("d1")
                .where("d1.name").notEq("D2")
            .startIntersect()
                    .from(Document.class, "d2")
                    .select("d2")
                    .where("d2.name").notEq("D3")
                .union()
                    .from(Document.class, "d3")
                    .select("d3")
                    .where("d3.name").eq("D1")
            .endSet()
        .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name <> :param_0\n"
                + "INTERSECT\n"
                + "(SELECT d2 FROM Document d2 WHERE d2.name <> :param_1\n"
                + "UNION\n"
                + "SELECT d3 FROM Document d3 WHERE d3.name = :param_2)";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testRightNesting() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class)
                .from(Document.class, "d1")
                .select("d1")
                .where("d1.name").eq("D1")
            .startExcept()
                    .from(Document.class, "d2")
                    .select("d2")
                    .where("d2.name").eq("D2")
                .startUnion()
                        .from(Document.class, "d3")
                        .select("d3")
                        .where("d3.name").eq("D3")
                    .union()
                        .from(Document.class, "d4")
                        .select("d4")
                        .where("d4.name").eq("D4")
                .endSet()
                .union()
                    .from(Document.class, "d5")
                    .select("d5")
                    .where("d5.name").eq("D5")
                .endSet()
            .union()
                .from(Document.class, "d6")
                .select("d6")
                .where("d6.name").eq("D6")
            .endSet();
        String expected = ""
                + "SELECT d1 FROM Document d1 WHERE d1.name = :param_0\n"
                + "EXCEPT\n"
                + "(SELECT d2 FROM Document d2 WHERE d2.name = :param_1\n"
                + "UNION\n"
                + "(SELECT d3 FROM Document d3 WHERE d3.name = :param_2\n"
                + "UNION\n"
                + "SELECT d4 FROM Document d4 WHERE d4.name = :param_3)\n"
                + "UNION\n"
                + "SELECT d5 FROM Document d5 WHERE d5.name = :param_4)\n"
                + "UNION\n"
                + "SELECT d6 FROM Document d6 WHERE d6.name = :param_5";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testLeftNesting() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
            .startSet(em, Document.class)
                .startSet()
                    .startSet()
                        .from(Document.class, "d1")
                        .select("d1")
                        .where("d1.name").eq("D1")
                    .intersect()
                        .from(Document.class, "d2")
                        .select("d2")
                        .where("d2.name").eq("D2")
                    .endSet()
                .union()
                    .from(Document.class, "d3")
                    .select("d3")
                    .where("d3.name").eq("D3")
                .endSet()
            .union()
                .from(Document.class, "d4")
                .select("d4")
                .where("d4.name").eq("D4")
            .endSet()
        .union()
            .from(Document.class, "d5")
            .select("d5")
            .where("d5.name").eq("D5")
        .endSet();
        String expected = ""
                + "(((SELECT d1 FROM Document d1 WHERE d1.name = :param_0\n"
                + "INTERSECT\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name = :param_1)\n"
                + "UNION\n"
                + "SELECT d3 FROM Document d3 WHERE d3.name = :param_2)\n"
                + "UNION\n"
                + "SELECT d4 FROM Document d4 WHERE d4.name = :param_3)\n"
                + "UNION\n"
                + "SELECT d5 FROM Document d5 WHERE d5.name = :param_4";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D3", resultList.get(0).getName());
    }
    
    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testLeftRightNesting() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
            .startSet(em, Document.class)
                .startSet()
                    .startSet()
                        .from(Document.class, "d1")
                        .select("d1")
                        .where("d1.name").eq("D1")
                        .startExcept()
                            .from(Document.class, "d2")
                            .select("d2")
                            .where("d2.name").eq("D2")
                            .startUnion()
                                .from(Document.class, "d3")
                                .select("d3")
                                .where("d3.name").eq("D3")
                                .startUnion()
                                    .from(Document.class, "d4")
                                    .select("d4")
                                    .where("d4.name").eq("D4")
                                .union()
                                    .from(Document.class, "d5")
                                    .select("d5")
                                    .where("d5.name").eq("D5")
                                .endSet()
                            .endSet()
                        .endSet()
                    .endSet()
                .endSet()
            .endSet()
            .union()
                .from(Document.class, "d6")
                .select("d6")
                .where("d6.name").eq("D6")
            .endSet();
        String expected = ""
                + "(((SELECT d1 FROM Document d1 WHERE d1.name = :param_0\n"
                + "EXCEPT\n"
                + "(SELECT d2 FROM Document d2 WHERE d2.name = :param_1\n"
                + "UNION\n"
                + "(SELECT d3 FROM Document d3 WHERE d3.name = :param_2\n"
                + "UNION\n"
                + "(SELECT d4 FROM Document d4 WHERE d4.name = :param_3\n"
                + "UNION\n"
                + "SELECT d5 FROM Document d5 WHERE d5.name = :param_4))))))\n"
                + "UNION\n"
                + "SELECT d6 FROM Document d6 WHERE d6.name = :param_5";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
    
    /* CTE set operations */
    
    // NOTE: H2 does not seem to support set operations in CTEs properly
    @Test
    @Category({ NoH2.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testCTENesting() {
        FinalSetOperationCriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d")
                .select("d")
                .with(IdHolderCTE.class)
                        .from(Document.class, "dCte1")
                        .bind("id").select("dCte1.id")
                        .where("dCte1.name").eq("D1")
                    .unionAll()
                        .from(Document.class, "dCte2")
                        .bind("id").select("dCte2.id")
                        .where("dCte2.name").eq("D2")
                    .except()
                        .from(Document.class, "dCte3")
                        .bind("id").select("dCte3.id")
                        .where("dCte3.name").eq("D3")
                        .startExcept()
                            .from(Document.class, "dCte4")
                            .bind("id").select("dCte4.id")
                            .where("dCte4.name").eq("D4")
                        .union()
                            .from(Document.class, "dCte5")
                            .bind("id").select("dCte5.id")
                            .where("dCte5.name").eq("D5")
                        .intersect()
                            .from(Document.class, "dCte6")
                            .bind("id").select("dCte6.id")
                            .where("dCte6.name").eq("D6")
                        .endSet()
                    .union()
                        .from(Document.class, "dCte7")
                        .bind("id").select("dCte7.id")
                        .where("dCte7.name").eq("D7")
                    .endSet()
                .end()
                .where("d.id").in()
                    .from(IdHolderCTE.class, "idHolder")
                    .select("idHolder.id")
                .end()
                .except()
                    .from(Document.class, "d2")
                    .select("d2")
                    .where("d2.name").eq("D2")
                .endSet();
        String expected = ""
                + "WITH IdHolderCTE(id) AS(\n"
                + "SELECT dCte1.id FROM Document dCte1 WHERE dCte1.name = :param_0\n"
                + "UNION ALL\n"
                + "SELECT dCte2.id FROM Document dCte2 WHERE dCte2.name = :param_1\n"
                + "EXCEPT\n"
                + "SELECT dCte3.id FROM Document dCte3 WHERE dCte3.name = :param_2\n"
                + "EXCEPT\n"
                + "(SELECT dCte4.id FROM Document dCte4 WHERE dCte4.name = :param_3\n"
                + "UNION\n"
                + "SELECT dCte5.id FROM Document dCte5 WHERE dCte5.name = :param_4\n"
                + "INTERSECT\n"
                + "SELECT dCte6.id FROM Document dCte6 WHERE dCte6.name = :param_5)\n"
                + "UNION\n"
                + "SELECT dCte7.id FROM Document dCte7 WHERE dCte7.name = :param_6\n"
                + ")\n"
                + "SELECT d FROM Document d WHERE d.id IN (SELECT idHolder.id FROM IdHolderCTE idHolder)\n"
                + "EXCEPT\n"
                + "SELECT d2 FROM Document d2 WHERE d2.name = :param_1";
        
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
    
    // NOTE: H2 does not seem to support set operations in CTEs properly
    @Test
    @Category({ NoH2.class, NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testCTELeftNesting() {
        CriteriaBuilder<Document> cb = cbf.create(em, Document.class)
                    .withStartSet(IdHolderCTE.class)
                        .startSet()
                            .from(Document.class, "d1")
                            .bind("id").select("d1.id")
                            .where("d1.name").eq("D1")
                        .except()
                            .from(Document.class, "d2")
                            .bind("id").select("d2.id")
                            .where("d2.name").eq("D2")
                        .endSet()
                        .startExcept()
                            .from(Document.class, "d3")
                            .bind("id").select("d3.id")
                            .where("d3.name").eq("D3")
                        .union()
                            .from(Document.class, "d4")
                            .bind("id").select("d4.id")
                            .where("d4.name").eq("D4")
                        .endSet()
                    .endSet()
                    .startExcept()
                        .startSet()
                            .from(Document.class, "d5")
                            .bind("id").select("d5.id")
                            .where("d5.name").eq("D5")
                        .union()
                            .from(Document.class, "d6")
                            .bind("id").select("d6.id")
                            .where("d6.name").eq("D6")
                        .endSet()
                    .endSet()
                .endSet()
                .end()
                .from(Document.class, "d")
                .from(IdHolderCTE.class, "idHolder")
                .select("d")
                .where("d.id").eqExpression("idHolder.id");
        String expected = ""
                + "WITH IdHolderCTE(id) AS(\n" +
                    "((SELECT d1.id FROM Document d1 WHERE d1.name = :param_0\n"
                    + "EXCEPT\n"
                    + "SELECT d2.id FROM Document d2 WHERE d2.name = :param_1)\n"
                    + "EXCEPT\n"
                    + "(SELECT d3.id FROM Document d3 WHERE d3.name = :param_2\n"
                    + "UNION\n"
                    + "SELECT d4.id FROM Document d4 WHERE d4.name = :param_3))\n"
                    + "EXCEPT\n"
                    + "((SELECT d5.id FROM Document d5 WHERE d5.name = :param_4\n"
                    + "UNION\n"
                    + "SELECT d6.id FROM Document d6 WHERE d6.name = :param_5))\n"
                + ")\n"
                + "SELECT d FROM Document d, IdHolderCTE idHolder WHERE d.id = idHolder.id";
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }

    /* Subquery set operations */

    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testSubqueryNesting() {
        CriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d")
                .select("d")
                .where("d.id").in()
                        .from(Document.class, "d1")
                        .select("d1.id")
                        .where("d1.name").eq("D1")
                    .except()
                        .from(Document.class, "d2")
                        .select("d2.id")
                        .where("d2.name").eq("D2")
                        .startExcept()
                            .from(Document.class, "d3")
                            .select("d3.id")
                            .where("d3.name").eq("D3")
                        .union()
                            .from(Document.class, "d4")
                            .select("d4.id")
                            .where("d4.name").eq("D4")
                        .intersect()
                            .from(Document.class, "d5")
                            .select("d5.id")
                            .where("d5.name").eq("D5")
                        .endSet()
                    .union()
                        .from(Document.class, "d6")
                        .select("d6.id")
                        .where("d6.name").eq("D6")
                    .endSet()
                .end();
        String expected = ""
                + "SELECT d FROM Document d WHERE d.id IN (" +
                      function(
                          "SET_UNION",
                          function(
                               "SET_EXCEPT", 
                               "(SELECT d1.id FROM Document d1 WHERE d1.name = :param_0)",
                               "(SELECT d2.id FROM Document d2 WHERE d2.name = :param_1)",
                               function(
                                    "SET_INTERSECT",
                                    function(
                                        "SET_UNION",
                                        "(SELECT d3.id FROM Document d3 WHERE d3.name = :param_2)",
                                        "(SELECT d4.id FROM Document d4 WHERE d4.name = :param_3)"
                                    ),
                                    "(SELECT d5.id FROM Document d5 WHERE d5.name = :param_4)"
                               )
                          ),
                          "(SELECT d6.id FROM Document d6 WHERE d6.name = :param_5)"
                      )
                + ")";
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }

    @Test
    @Category({ NoDatanucleus.class, NoEclipselink.class, NoOpenJPA.class, NoMySQL.class })
    public void testSubqueryLeftNesting() {
        CriteriaBuilder<Document> cb = cbf
                .create(em, Document.class, "d")
                .select("d")
                .where("d.id").in()
                    .startSet()
                        .startSet()
                            .from(Document.class, "d1")
                            .select("d1.id")
                            .where("d1.name").eq("D1")
                        .except()
                            .from(Document.class, "d2")
                            .select("d2.id")
                            .where("d2.name").eq("D2")
                        .endSet()
                        .startExcept()
                            .from(Document.class, "d3")
                            .select("d3.id")
                            .where("d3.name").eq("D3")
                        .union()
                            .from(Document.class, "d4")
                            .select("d4.id")
                            .where("d4.name").eq("D4")
                        .endSet()
                    .endSet()
                    .startExcept()
                        .startSet()
                            .from(Document.class, "d5")
                            .select("d5.id")
                            .where("d5.name").eq("D5")
                        .union()
                            .from(Document.class, "d6")
                            .select("d6.id")
                            .where("d6.name").eq("D6")
                        .endSet()
                    .endSet()
                .endSet()
                .end();
        String expected = ""
                + "SELECT d FROM Document d WHERE d.id IN (" +
                    function(
                         "SET_EXCEPT",
                        function(
                             "SET_EXCEPT",
                            function(
                                 "SET_EXCEPT",
                                 "(SELECT d1.id FROM Document d1 WHERE d1.name = :param_0)",
                                 "(SELECT d2.id FROM Document d2 WHERE d2.name = :param_1)"
                            ),
                            function(
                                 "SET_UNION",
                                 "(SELECT d3.id FROM Document d3 WHERE d3.name = :param_2)",
                                 "(SELECT d4.id FROM Document d4 WHERE d4.name = :param_3)"
                            )
                        ),
                        function(
                             "SET_UNION",
                             "(SELECT d5.id FROM Document d5 WHERE d5.name = :param_4)",
                             "(SELECT d6.id FROM Document d6 WHERE d6.name = :param_5)"
                        )
                    )
                + ")";
        assertEquals(expected, cb.getQueryString());
        List<Document> resultList = cb.getResultList();
        assertEquals(1, resultList.size());
        assertEquals("D1", resultList.get(0).getName());
    }
}
