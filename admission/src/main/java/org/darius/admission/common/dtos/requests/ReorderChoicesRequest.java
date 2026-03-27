package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderChoicesRequest {

    @NotEmpty(message = "La liste des choix est obligatoire")
    private List<ChoiceOrder> choices;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChoiceOrder {
        private Long choiceId;
        private int newOrder;
    }
}