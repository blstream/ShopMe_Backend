package com.intive.shopme.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import javax.persistence.*;
import java.util.UUID;

@Entity
@ApiModel(value = "Offer", description = "Represents the offer created by user")
@Builder
public @Data
class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ApiModelProperty(value = "Represents offer's unique id number", required = true)
    private UUID id;

    @ApiModelProperty(value = "Represents offer's date of submitting", required = true)
    private String date;

    @ApiModelProperty(value = "Represents offer's title - passed by user", required = true)
    private String title;

    @ApiModelProperty(value = "Represents offer's category", required = true)
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Category category;

    @ApiModelProperty(value = "Represents offer's bundle", required = true)
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Bundle bundle;

    @ApiModelProperty(value = "Represents the user who submits this offer", required = true)
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private User user;


}