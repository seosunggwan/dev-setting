package com.example.backend.item.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("Movie")
@Getter
@Setter
public class Movie extends Item {

    private String director;
    private String actor;
}
