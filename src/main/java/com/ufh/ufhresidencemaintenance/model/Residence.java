package com.ufh.ufhresidencemaintenance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "residences")
public class Residence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private ResidenceZone zone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResidenceZone getZone() {
        return zone;
    }

    public void setZone(ResidenceZone zone) {
        this.zone = zone;
    }
}
