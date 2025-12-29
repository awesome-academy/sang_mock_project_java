package com.example.ems.util;

import com.example.ems.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class ObjectDiffer {

    public String diff(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) return "";
        
        List<String> changes = new ArrayList<>();
        Field[] fields = oldObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                String fieldName = field.getName();
                
                if (isIgnoredField(fieldName)) continue;

                if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                Object val1 = field.get(oldObj);
                Object val2 = field.get(newObj);

                if (val1 instanceof BigDecimal && val2 instanceof BigDecimal) {
                    if (((BigDecimal) val1).compareTo((BigDecimal) val2) != 0) {
                        changes.add(String.format("%s: %s -> %s", fieldName, val1, val2));
                    }
                    continue;
                }

                if (val1 instanceof BaseEntity || val2 instanceof BaseEntity) {
                	UUID id1 = (val1 instanceof BaseEntity) ? ((BaseEntity) val1).getId() : null;
                    UUID id2 = (val2 instanceof BaseEntity) ? ((BaseEntity) val2).getId() : null;

                    if (!Objects.equals(id1, id2)) {
                        changes.add(fieldName + " changed"); 
                    }
                    continue;
                }
                
                if (!Objects.equals(val1, val2)) {
                     changes.add(String.format("%s: '%s' -> '%s'", fieldName, val1, val2));
                }

            } catch (IllegalAccessException e) {
                log.warn("Could not access field: {}", field.getName());
            } catch (Exception e) {
                log.warn("Error comparing field {}: {}", field.getName(), e.getMessage());
            }
        }
        return String.join(", ", changes);
    }

    private boolean isIgnoredField(String name) {
        return List.of("id", "createdAt", "updatedAt", "serialVersionUID").contains(name);
    }
}
