package com.ufh.ufhresidencemaintenance.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "residence_zones")
public class ResidenceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean active;

    @OneToMany(mappedBy = "zone")
    private List<Residence> residences;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Residence> getResidences() {
        return residences;
    }

    public void setResidences(List<Residence> residences) {
        this.residences = residences;
    }
}
