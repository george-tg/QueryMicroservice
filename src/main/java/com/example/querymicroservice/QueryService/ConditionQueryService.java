package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.QueryRepository.ConditionRepository;
import com.example.querymicroservice.domain.Condition;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.ConditionDTO;
import com.example.querymicroservice.dtos.ObservationDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConditionQueryService
{
    private static final Logger logger = LoggerFactory.getLogger(ConditionQueryService.class);

    @Autowired
    private ConditionRepository repository;

    public ConditionDTO consumeReadEvent(Long id) {
        Condition condition = repository.findById(id).orElse(null);
        if (condition != null) {
            ConditionDTO conditionDTO = convertConditionToDTO(condition);
            logger.info("Getting condition: "+ condition.getId() +condition.getConditionName());
            return conditionDTO;
        } else {
            System.out.println("Condition not found for ID: " + id);
            return null;
        }
    }
    static ConditionDTO convertConditionToDTO(Condition condition) {

        return new ConditionDTO(condition.getId(), condition.getConditionName(), PatientQueryService.convertToDTO(condition.getPatient()));
    }



    @KafkaListener(topics = "delete_condition_event", groupId = "condition_group")
    public void handleDeleteConditionEvent(Long id) {
        repository.deleteById(id);
    }

    @KafkaListener(topics = "create_condition_event", groupId = "condition_group")
    public void handleCreateConditionEvent(ConditionDTO conditionDTO) {
        Condition condition = new Condition(conditionDTO.getConditionName(), new Patient(conditionDTO.getPatient().getFirstName(),conditionDTO.getPatient().getLastName(),conditionDTO.getPatient().getAge()));
        repository.save(condition);
    }

    @KafkaListener(topics = "update_condition_event", groupId = "condition_group")
    public void handleUpdateConditionEvent(ConditionDTO conditionDTO)
    {
        Long id = conditionDTO.getId();

        Patient p = new Patient(conditionDTO.getPatient().getId(),conditionDTO.getPatient().getFirstName(),conditionDTO.getPatient().getLastName(),conditionDTO.getPatient().getAge());
        Condition condition = new Condition(conditionDTO.getConditionName(), p);
        Optional<Condition> existingCondition = repository.findById(id);
        if (existingCondition.isPresent()) {
            condition.setId(id);
            repository.save(condition);
        } else {
            throw new RuntimeException("Can't update condition " + id);
        }
    }


}
