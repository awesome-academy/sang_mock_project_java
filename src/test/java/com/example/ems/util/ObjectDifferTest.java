package com.example.ems.util;

import com.example.ems.entity.BaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectDifferTest {

    private final ObjectDiffer objectDiffer = new ObjectDiffer();

    static class MockEntity extends BaseEntity {
        private String name;
        private BigDecimal amount;
        private MockRelation relatedEntity;
        private List<String> tags; // Collection should be ignored
        
        // Constructor & Setters
        public MockEntity(UUID id, String name, BigDecimal amount) {
            this.setId(id);
            this.name = name;
            this.amount = amount;
        }
        
        public void setRelatedEntity(MockRelation r) { this.relatedEntity = r; }
        public void setTags(List<String> t) { this.tags = t; }
    }

    static class MockRelation extends BaseEntity {
        public MockRelation(UUID id) {
            this.setId(id);
        }
    }

    // --- TEST CASES ---

    @Test
    @DisplayName("Diff - Standard Fields (String, Integer) Changed")
    void testDiff_StandardFieldsChanged() {
        // Given
        UUID id = UUID.randomUUID();
        MockEntity oldObj = new MockEntity(id, "Old Name", BigDecimal.TEN);
        MockEntity newObj = new MockEntity(id, "New Name", BigDecimal.TEN);

        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertEquals("name: 'Old Name' -> 'New Name'", result);
    }

    @Test
    @DisplayName("Diff - BigDecimal Comparison (Ignore Scale)")
    void testDiff_BigDecimal() {
        // Given
        UUID id = UUID.randomUUID();
        MockEntity oldObj = new MockEntity(id, "A", new BigDecimal("100.00"));
        MockEntity newObj = new MockEntity(id, "A", new BigDecimal("100.0"));

        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertTrue(result.isEmpty(), "BigDecimal equal in value but different scale should not be logged");

        newObj.amount = new BigDecimal("200");
        String result2 = objectDiffer.diff(oldObj, newObj);
        assertEquals("amount: 100.00 -> 200", result2);
    }

    @Test
    @DisplayName("Diff - BaseEntity Relation Changed (Compare by ID)")
    void testDiff_RelationChanged() {
        // Given
        UUID id = UUID.randomUUID();
        MockEntity oldObj = new MockEntity(id, "A", BigDecimal.ZERO);
        MockEntity newObj = new MockEntity(id, "A", BigDecimal.ZERO);

        MockRelation rel1 = new MockRelation(UUID.randomUUID());
        MockRelation rel2 = new MockRelation(UUID.randomUUID());

        oldObj.setRelatedEntity(rel1);
        newObj.setRelatedEntity(rel2); 

        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertEquals("relatedEntity changed", result);
    }

    @Test
    @DisplayName("Diff - BaseEntity Relation Null Handling")
    void testDiff_RelationNull() {
        // Given
        MockEntity oldObj = new MockEntity(UUID.randomUUID(), "A", BigDecimal.ZERO);
        MockEntity newObj = new MockEntity(UUID.randomUUID(), "A", BigDecimal.ZERO);

        oldObj.setRelatedEntity(new MockRelation(UUID.randomUUID()));
        newObj.setRelatedEntity(null);

        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertEquals("relatedEntity changed", result);
    }

    @Test
    @DisplayName("Diff - Ignored Fields (ID, CreatedAt...)")
    void testDiff_IgnoredFields() {
        // Given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID(); 
        MockEntity oldObj = new MockEntity(id1, "A", BigDecimal.TEN);
        MockEntity newObj = new MockEntity(id2, "A", BigDecimal.TEN);

        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertTrue(result.isEmpty(), "Changes in ID should be ignored");
    }

    @Test
    @DisplayName("Diff - Collections Should Be Ignored")
    void testDiff_CollectionsIgnored() {
        // Given
        MockEntity oldObj = new MockEntity(UUID.randomUUID(), "A", BigDecimal.TEN);
        MockEntity newObj = new MockEntity(UUID.randomUUID(), "A", BigDecimal.TEN);

        oldObj.setTags(List.of("tag1"));
        newObj.setTags(List.of("tag1", "tag2"));
        // When
        String result = objectDiffer.diff(oldObj, newObj);

        // Then
        assertTrue(result.isEmpty(), "Collections should be skipped to avoid LazyInitializationException");
    }

    @Test
    @DisplayName("Diff - Null Inputs")
    void testDiff_NullInputs() {
        assertEquals("", objectDiffer.diff(null, new MockEntity(null, null, null)));
        assertEquals("", objectDiffer.diff(new MockEntity(null, null, null), null));
    }
}
