package ru.akutepov.exchangeratesbot.repositry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.akutepov.exchangeratesbot.entity.FileEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID>, JpaSpecificationExecutor<FileEntity> {

    @Query("SELECT DISTINCT f.fileType FROM FileEntity f WHERE f.isDeleted = false")
    List<String> findDistinctFileTypes();
}
