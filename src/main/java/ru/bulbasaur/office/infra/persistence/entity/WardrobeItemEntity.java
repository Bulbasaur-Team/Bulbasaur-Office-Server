package ru.bulbasaur.office.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wardrobe_items")
@Getter
@Setter
@NoArgsConstructor
public class WardrobeItemEntity {

    @Id
    private String code;

    private String category;

    private String name;

    private long price;

    @jakarta.persistence.Column(name = "sort_order")
    private int sortOrder;

    private boolean sellable;
}
