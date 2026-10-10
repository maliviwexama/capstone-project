package com.ufh.ufhresidencemaintenance.config;

import com.ufh.ufhresidencemaintenance.model.Residence;
import com.ufh.ufhresidencemaintenance.model.ResidenceZone;
import com.ufh.ufhresidencemaintenance.repository.ResidenceRepository;
import com.ufh.ufhresidencemaintenance.repository.ResidenceZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds the official residence structure supplied for the 2026 system.
 *
 * The four top-level entries are Residence Zones. The names underneath each
 * zone are the actual residence choices shown during registration.
 * Existing matching records are reused; missing records are added.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final ResidenceRepository residenceRepository;
    private final ResidenceZoneRepository residenceZoneRepository;

    public DataInitializer(ResidenceRepository residenceRepository,
                           ResidenceZoneRepository residenceZoneRepository) {
        this.residenceRepository = residenceRepository;
        this.residenceZoneRepository = residenceZoneRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, String[]> officialResidences = new LinkedHashMap<>();

        officialResidences.put("Mans", new String[]{
                "Wesley", "Marikana", "Nkandla", "Jolobe 1", "Jolobe 2",
                "Jabavu", "Iona 1", "Iona 2", "ZK Matthews 1", "ZK Matthews 2"
        });

        officialResidences.put("East Camp", new String[]{
                "Taunyane", "Tiyo 1", "Tiyo 2", "Tiyo 3", "Tyhali 1", "Tyhali 2"
        });

        officialResidences.put("Mzana", new String[]{
                "Eluk 1", "Eluk 2", "Eluk 3", "Elitheni 1", "Elitheni 2",
                "Ezola 1", "Ezola 2", "Ezola 3", "Kuwait 1", "Kuwait 2",
                "Ethembeni", "Emfundweni"
        });

        officialResidences.put("Student Village", new String[]{
                "SV 1.1", "SV 1.2", "SV 1.3",
                "SV 2.1", "SV 2.2", "SV 2.3",
                "SV 3.1", "SV 3.2", "SV 3.3",
                "SV 4.1", "SV 4.2", "SV 4.3",
                "SV 5.1", "SV 5.2", "SV 5.3",
                "SV 6.1", "SV 6.2", "SV 6.3"
        });

        for (Map.Entry<String, String[]> zoneEntry : officialResidences.entrySet()) {
            ResidenceZone zone = residenceZoneRepository.findAll().stream()
                    .filter(existing -> zoneEntry.getKey().equalsIgnoreCase(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        ResidenceZone newZone = new ResidenceZone();
                        newZone.setName(zoneEntry.getKey());
                        // Only Mzana is the initial active zone on a fresh database.
                        // Existing zones keep their current active/inactive state.
                        newZone.setActive("Mzana".equalsIgnoreCase(zoneEntry.getKey()));
                        return residenceZoneRepository.save(newZone);
                    });

            // Do not overwrite the zone's active state on every application restart.
            // Zone activation is an admin-controlled setting and must persist in the DB.

            for (String residenceName : zoneEntry.getValue()) {
                Residence residence = residenceRepository.findAll().stream()
                        .filter(existing -> residenceName.equalsIgnoreCase(existing.getName()))
                        .findFirst()
                        .orElseGet(() -> {
                            Residence newResidence = new Residence();
                            newResidence.setName(residenceName);
                            return newResidence;
                        });

                // Reassign existing matching residences to the correct zone.
                residence.setName(residenceName);
                residence.setZone(zone);
                residenceRepository.save(residence);
            }
        }
    }
}
