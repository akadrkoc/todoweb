package com.example.todo.repository;

import com.example.todo.model.Todo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Methods for Drag & Drop sorting
    List<Todo> findAllByOrderByCompletedAscDisplayOrderAscCreatedAtDesc();
    
    @Query("SELECT MAX(t.displayOrder) FROM Todo t WHERE t.completed = :completed")
    Integer findMaxDisplayOrderByCompleted(@Param("completed") boolean completed);
    
    @Modifying
    @Query("UPDATE Todo t SET t.displayOrder = t.displayOrder + 1 WHERE t.completed = :completed AND t.displayOrder >= :fromOrder")
    void incrementDisplayOrderFrom(@Param("completed") boolean completed, @Param("fromOrder") int fromOrder);
    
    @Modifying
    @Query("UPDATE Todo t SET t.displayOrder = t.displayOrder - 1 WHERE t.completed = :completed AND t.displayOrder > :fromOrder AND t.displayOrder <= :toOrder")
    void decrementDisplayOrderBetween(@Param("completed") boolean completed, @Param("fromOrder") int fromOrder, @Param("toOrder") int toOrder);
    
    @Query("SELECT t FROM Todo t WHERE t.completed = :completed ORDER BY t.displayOrder ASC, t.createdAt DESC")
    List<Todo> findByCompletedOrderByDisplayOrderAscCreatedAtDesc(@Param("completed") boolean completed);
}