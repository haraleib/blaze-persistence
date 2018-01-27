/*
 * Copyright 2014 - 2018 Blazebit.
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

package com.blazebit.persistence.spring.data.impl;

import com.blazebit.persistence.spring.data.impl.accessor.DocumentAccessor;
import com.blazebit.persistence.spring.data.impl.accessor.DocumentAccessors;
import com.blazebit.persistence.spring.data.impl.config.SystemPropertyBasedActiveProfilesResolver;
import com.blazebit.persistence.spring.data.impl.entity.Document;
import com.blazebit.persistence.spring.data.impl.entity.Person;
import com.blazebit.persistence.spring.data.impl.repository.DocumentEntityRepository;
import com.blazebit.persistence.spring.data.impl.repository.DocumentRepository;
import com.blazebit.persistence.spring.data.impl.repository.DocumentViewRepository;
import com.blazebit.persistence.spring.data.impl.repository.EntityViewRepositoryFactoryBean;
import com.blazebit.persistence.spring.data.impl.tx.TransactionalWorkService;
import com.blazebit.persistence.spring.data.impl.tx.TxWork;
import com.blazebit.persistence.view.impl.spring.EnableEntityViews;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Moritz Becker (moritz.becker@gmx.at)
 * @since 1.2
 */
@RunWith(Parameterized.class)
@ContextConfiguration(classes = DocumentRepositoryTest.TestConfig.class)
@ActiveProfiles(resolver = SystemPropertyBasedActiveProfilesResolver.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DocumentRepositoryTest extends AbstractSpringTest {

    @Parameterized.Parameters
    public static Iterable<?> createParameters() {
        return Arrays.asList(DocumentViewRepository.class, DocumentEntityRepository.class);
    }

    private final Class<? extends DocumentRepository<?>> repositoryClass;

    @Autowired
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Autowired
    private TransactionalWorkService transactionalWorkService;

    private DocumentRepository<?> documentRepository;

    public DocumentRepositoryTest(Class<? extends DocumentRepository<?>> repositoryClass) {
        this.repositoryClass = repositoryClass;
    }

    @Before
    public void initRepository() {
        documentRepository = autowireCapableBeanFactory.getBean(repositoryClass);
    }

    @Test
    public void testFindOne() {
        // Given
        final Document d1 = createDocument("D1");
        final Document d2 = createDocument("D2");

        // When
        DocumentAccessor result1 = DocumentAccessors.of(documentRepository.findOne(d1.getId()));
        DocumentAccessor result2 = DocumentAccessors.of(documentRepository.findOne(d2.getId()));

        // Then
        assertEquals(d1.getId(), result1.getId());
        assertNotNull(result1);

        assertEquals(d2.getId(), result2.getId());
        assertNotNull(result2);
    }

    @Test
    public void testExists(){
        // Given
        final Document d1 = createDocument("D1");

        // When
        boolean existsP1 = documentRepository.exists(d1.getId());

        // Then
        assertTrue(existsP1);
    }

    @Test
    public void testFindAll() {
        // Given
        final Person p1 = createPerson("P1");
        final Document d1 = createDocument("D1", p1);
        final Document d2 = createDocument("D2", p1);

        // When
        Iterable<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findAll());
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actualIds.size());
        assertTrue(actualIds.contains(d1.getId()));
        assertTrue(actualIds.contains(d2.getId()));
    }

    @Test
    public void testFindAllByIds() {
        // ignored with EclipseLink due to IN collection rendering bug
        Assume.assumeFalse(isEntityRepository() && isEclipseLink());
        // Given
        final Document d1 = createDocument("D1");
        final Document d2 = createDocument("D2");

        // When
        Iterable<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findAll(Arrays.asList(d1.getId(), d2.getId())));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actualIds.size());
        assertTrue(actualIds.contains(d1.getId()));
        assertTrue(actualIds.contains(d2.getId()));
    }

    @Test
    public void testCount() {
        // Given
        final Document d1 = createDocument("D1");
        final Document d2 = createDocument("D2");

        // When
        long count = documentRepository.count();

        // Then
        assertEquals(2, count);
    }

    @Test
    public void testFindByName() {
        // Given
        final Document d1 = createDocument("D1");

        // When
        List<DocumentAccessor> result = DocumentAccessors.of(documentRepository.findByName(d1.getName()));

        // Then
        assertEquals(1, result.size());
        assertEquals(d1.getId(), result.get(0).getId());
    }

    @Test
    public void testFindByNameAndAgeOrDescription() {
        // Given
        final String name = "D1";
        final Document d1 = createDocument(name, "desc1", 12, null);
        final Document d2 = createDocument(name, "desc2", 13, null);
        final Document d3 = createDocument(name, "desc3", 14, null);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByNameAndAgeOrDescription(name, 12, "desc2"));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.size());
        assertTrue(actualIds.contains(d1.getId()));
        assertTrue(actualIds.contains(d2.getId()));
    }

    @Test
    public void testFindByNameIn() {
        // ignored with EclipseLink due to IN collection rendering bug
        Assume.assumeFalse(isEntityRepository() && isEclipseLink());
        // Given
        final Document d1 = createDocument("d1");
        final Document d2 = createDocument("d2");
        final Document d3 = createDocument("d3");

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByNameIn(d2.getName(), d3.getName()));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.size());
        assertTrue(actualIds.contains(d2.getId()));
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindByNameInPaginated() {
        // ignored with EclipseLink due to IN collection rendering bug
        Assume.assumeFalse(isEntityRepository() && isEclipseLink());
        // Given
        final Document d1 = createDocument("d1");
        final Document d2 = createDocument("d2");
        final Document d3 = createDocument("d3");

        // When
        Page<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByNameInOrderById(new PageRequest(0, 1), d2.getName(), d3.getName()));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.getTotalPages());
        assertEquals(0, actual.getNumber());
        assertEquals(1, actual.getNumberOfElements());
        assertEquals(1, actual.getSize());
        assertTrue(actualIds.contains(d2.getId()));

        actual = DocumentAccessors.of(documentRepository.findByNameInOrderById(actual.nextPageable(), d2.getName(), d3.getName()));
        actualIds = getIdsFromViews(actual);
        assertEquals(2, actual.getTotalPages());
        assertEquals(1, actual.getNumber());
        assertEquals(1, actual.getNumberOfElements());
        assertEquals(1, actual.getSize());
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindByNameLikeOrderByAgeAsc() {
        // Given
        final Document d1 = createDocument("d1", null, 2l, null);
        final Document d2 = createDocument("d2", null, 1l, null);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByNameLikeOrderByAgeAsc("d%"));

        // Then
        assertEquals(2, actual.size());
        assertEquals(d2.getId(), actual.get(0).getId());
        assertEquals(d1.getId(), actual.get(1).getId());
    }

    @Test
    public void testFindByOwnerName() {
        // Given
        final Person p1 = createPerson("p1");
        final Person p2 = createPerson("p2");

        final Document d1 = createDocument("d1", p1);
        final Document d2 = createDocument("d2", p2);
        final Document d3 = createDocument("d3", p2);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByOwnerName(p2.getName()));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.size());
        assertTrue(actualIds.contains(d2.getId()));
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindByAgeGreaterThanEqual() {
        // Given
        final Document d1 = createDocument("d1", null, 3l, null);
        final Document d2 = createDocument("d2", null, 4l, null);
        final Document d3 = createDocument("d3", null, 5l, null);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findByAgeGreaterThanEqual(4l));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.size());
        assertTrue(actualIds.contains(d2.getId()));
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindSliceByAgeGreaterThanEqual() {
        // Given
        final Document d1 = createDocument("d1", null, 3l, null);
        final Document d2 = createDocument("d2", null, 4l, null);
        final Document d3 = createDocument("d3", null, 5l, null);

        // When
        Slice<DocumentAccessor> actual = DocumentAccessors.of(documentRepository.findSliceByAgeGreaterThanEqual(4l, new PageRequest(1, 1)));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(1, actual.getSize());
        assertFalse(actual.hasNext());
        assertTrue(actual.hasPrevious());
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindFirstByOrderByNameAsc() {
        // Given
        final Document d3 = createDocument("d3");
        final Document d2 = createDocument("d2");
        final Document d1 = createDocument("d1");

        // When
        DocumentAccessor actual = DocumentAccessors.of(documentRepository.findFirstByOrderByNameAsc());

        // Then
        assertEquals(d1.getId(), actual.getId());
    }

    @Test
    public void testFindByAgeIn() {
        // ignored with EclipseLink due to IN collection rendering bug
        Assume.assumeFalse(isEclipseLink());
        // Given
        final Document d3 = createDocument("d3");
        final Document d2 = createDocument("d2");
        final Document d1 = createDocument("d1");

        // When

        List<DocumentAccessor> actual1 = DocumentAccessors.of(documentRepository.findByNameIn(new HashSet<String>(0)));
        List<DocumentAccessor> actual2 = DocumentAccessors.of(documentRepository.findByAgeIn(new Long[0]));

        // Then
        assertEquals(0, actual1.size());
        assertEquals(0, actual2.size());
    }

    @Test
    public void testFindOneBySpec() {
        // Given
        final Document d3 = createDocument("d3", null, 3L, null);
        final Document d2 = createDocument("d2", null, 2L, null);
        final Document d1 = createDocument("d1", null, 1L, null);

        // When
        DocumentAccessor actual = DocumentAccessors.of(((DocumentRepository) documentRepository).findOne(new Specification<Document>() {
            @Override
            public Predicate toPredicate(Root<Document> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.equal(root.<String>get("name"), "d2");
            }
        }));

        // Then
        assertTrue(actual.getId().equals(d2.getId()));
    }

    @Test
    public void testFindAllBySpec() {
        // Given
        final Document d3 = createDocument("d3", null, 3L, null);
        final Document d2 = createDocument("d2", null, 2L, null);
        final Document d1 = createDocument("d1", null, 1L, null);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(((DocumentRepository) documentRepository).findAll(new Specification<Document>() {
            @Override
            public Predicate toPredicate(Root<Document> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.ge(root.<Long>get("age"), 2L);
            }
        }));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(2, actual.size());
        assertTrue(actualIds.contains(d2.getId()));
        assertTrue(actualIds.contains(d3.getId()));
    }

    @Test
    public void testFindAllBySpecPageable() {
        // Given
        final Document d4 = createDocument("d4", null, 2L, null);
        final Document d3 = createDocument("d3", null, 3L, null);
        final Document d2 = createDocument("d2", null, 2L, null);
        final Document d1 = createDocument("d1", null, 1L, null);

        // When
        Page<DocumentAccessor> actual = DocumentAccessors.of(((DocumentRepository) documentRepository).findAll(new Specification<Document>() {
            @Override
            public Predicate toPredicate(Root<Document> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.gt(root.<Long>get("age"), 1L);
            }
        }, new PageRequest(1, 2, Sort.Direction.ASC, "name", "id")));
        List<Long> actualIds = getIdsFromViews(actual);

        // Then
        assertEquals(1, actual.getNumberOfElements());
        assertTrue(actualIds.contains(d4.getId()));
    }

    @Test
    public void testFindAllBySpecSorted() {
        // Given
        final Document d3 = createDocument("d3", null, 3L, null);
        final Document d2 = createDocument("d2", null, 2L, null);
        final Document d1 = createDocument("d1", null, 1L, null);

        // When
        List<DocumentAccessor> actual = DocumentAccessors.of(((DocumentRepository) documentRepository).findAll(new Specification<Document>() {
            @Override
            public Predicate toPredicate(Root<Document> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                return criteriaBuilder.ge(root.<Long>get("age"), 2L);
            }
        }, new Sort(Sort.Direction.ASC, "name")));

        // Then
        assertEquals(2, actual.size());
        assertTrue(actual.get(0).getId().equals(d2.getId()));
        assertTrue(actual.get(1).getId().equals(d3.getId()));
    }

    private List<Long> getIdsFromViews(Iterable<DocumentAccessor> views) {
        List<Long> ids = new ArrayList<>();
        for (DocumentAccessor view : views) {
            ids.add(view.getId());
        }
        return ids;
    }

    private Document createDocument(String name) {
        return createDocument(name, null);
    }

    private Document createDocument(final String name, final Person owner) {
        return createDocument(name, null, 0l, owner);
    }

    private Document createDocument(final String name, final String description, final long age, final Person owner) {
        return transactionalWorkService.doTxWork(new TxWork<Document>() {
            @Override
            public Document work(EntityManager em) {
                Document d = new Document(name);
                d.setDescription(description);
                d.setAge(age);
                d.setOwner(owner);
                em.persist(d);
                return d;
            }
        });
    }

    private Person createPerson(final String name) {
        return transactionalWorkService.doTxWork(new TxWork<Person>() {
            @Override
            public Person work(EntityManager em) {
                Person p = new Person(name);
                em.persist(p);
                return p;
            }
        });
    }

    private boolean isEntityRepository() {
        return repositoryClass == DocumentEntityRepository.class;
    }

    private boolean isEclipseLink() {
        return Arrays.asList(new SystemPropertyBasedActiveProfilesResolver().resolve(DocumentRepositoryTest.class)).contains("eclipselink");
    }

    @Configuration
    @ComponentScan
    @ImportResource("classpath:/com/blazebit/persistence/spring/data/impl/application-config.xml")
    @EnableEntityViews(basePackages = {"org.springframework.data.jpa.repository.support", "com.blazebit.persistence.spring.data.impl.view"})
    @EnableJpaRepositories(
            basePackages = "com.blazebit.persistence.spring.data.impl.repository",
            entityManagerFactoryRef = "myEmf",
            repositoryFactoryBeanClass = EntityViewRepositoryFactoryBean.class)
    static class TestConfig {
    }
}