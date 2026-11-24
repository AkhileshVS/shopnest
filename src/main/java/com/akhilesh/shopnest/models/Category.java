package com.akhilesh.shopnest.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category extends BaseModel {

    @Column(nullable = false, unique = true)
    private String name;

    private String description;
}
