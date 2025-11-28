package com.sxxm.stockknock.dto;

import com.sxxm.stockknock.entity.User;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private User.InvestmentStyle investmentStyle;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setInvestmentStyle(user.getInvestmentStyle());
        return dto;
    }
}

