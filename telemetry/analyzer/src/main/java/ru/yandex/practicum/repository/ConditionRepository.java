package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;

import java.util.List;

@Repository
public interface ConditionRepository extends JpaRepository<Condition, Long> {
    List<Condition> findAllByScenario(Scenario scenario);

    void deleteByScenario(Scenario scenario);

    @Query("SELECT c FROM Condition c WHERE c.sensor.id = :sensorId")
    List<Condition> findBySensorId(@Param("sensorId") String sensorId);

    @Modifying
    @Query("DELETE FROM Condition c WHERE c.sensor.id = :sensorId")
    void deleteBySensorId(@Param("sensorId") String sensorId);
}