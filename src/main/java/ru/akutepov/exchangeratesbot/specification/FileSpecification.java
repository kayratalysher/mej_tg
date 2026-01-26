package ru.akutepov.exchangeratesbot.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.akutepov.exchangeratesbot.entity.FileEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileSpecification {

    public static Specification<FileEntity> search(Map<String, Object> filters) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Object valueObj = filters.get("value");
            if (valueObj instanceof String value && !value.isBlank()) {
                String search = "%" + value.trim().toLowerCase() + "%";
                Predicate filenamePredicate = cb.like(cb.lower(root.get("filename")), search);
                predicates.add(cb.or(filenamePredicate));
            }

            Object valueObj4 = filters.get("fileType");
            if (valueObj4 instanceof String value && !value.isBlank()) {
                String search = value.trim().toLowerCase();
                predicates.add(cb.equal(cb.lower(root.get("fileType")), search));
            }


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
