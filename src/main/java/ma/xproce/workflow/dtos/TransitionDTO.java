package ma.xproce.workflow.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitionDTO {
    private String name;
    private Long sourceStatutId;
    private Long targetStatutId;
    private String conditionExpression;
}