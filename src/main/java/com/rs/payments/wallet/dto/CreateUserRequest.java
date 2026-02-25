package com.rs.payments.wallet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request to create a new user")
public class CreateUserRequest {

    @NotBlank
    @Schema(description = "Username of the user", example = "johndoe")
    private String username;

//    @NotBlank
//    @Schema(description = "Email address of the user", example = "john.doe@example.com")
//    private String email;

    @NotBlank
    @Email
    private String email;
}
