package com.example.todo.repository;

import com.example.todo.model.Todo;
import com.example.todo.model.user;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    // Tasks for a specific user, ordered by completion, display order, and creation date
    List<Todo> findAllByUserOrderByCompletedAscDisplayOrderAscCreatedAtDesc(user user);

    // User's maximum displayOrder for completed or not completed tasks
    @Query("SELECT MAX(t.displayOrder) FROM Todo t WHERE t.user = :user AND t.completed = :completed")
    Integer findMaxDisplayOrderByUserAndCompleted(@Param("user") user user,
                                                  @Param("completed") boolean completed);

    // displayOrder for a specific task of a user (increment)
    @Modifying
    @Query("UPDATE Todo t SET t.displayOrder = t.displayOrder + 1 " +
            "WHERE t.user = :user AND t.completed = :completed AND t.displayOrder >= :fromOrder")
    void incrementDisplayOrderFrom(@Param("user") user user,
                                   @Param("completed") boolean completed,
                                   @Param("fromOrder") int fromOrder);

    // displayOrder for a specific task of a user (decrement)
    @Modifying
    @Query("UPDATE Todo t SET t.displayOrder = t.displayOrder - 1 " +
            "WHERE t.user = :user AND t.completed = :completed " +
            "AND t.displayOrder > :fromOrder AND t.displayOrder <= :toOrder")
    void decrementDisplayOrderBetween(@Param("user") user user,
                                      @Param("completed") boolean completed,
                                      @Param("fromOrder") int fromOrder,
                                      @Param("toOrder") int toOrder);

    // User's tasks by completion status, ordered by displayOrder and createdAt
    @Query("SELECT t FROM Todo t WHERE t.user = :user AND t.completed = :completed " +
            "ORDER BY t.displayOrder ASC, t.createdAt DESC")
    List<Todo> findByUserAndCompletedOrderByDisplayOrderAscCreatedAtDesc(@Param("user") user user,
                                                                         @Param("completed") boolean completed);
}
