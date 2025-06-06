package com.example.bitcoin.face.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FaceEvaluateDTO {
    private BigDecimal remainingBalance;
}
