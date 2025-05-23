package com.itaul.rofm.model;

import com.itaul.rofm.model.enums.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "location")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
//    @Column(nullable = false, columnDefinition = "CHAR(36)")
//    @Column(nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(length = 30)
    private String title;

    @Column()
    private Double latitude;

    @Column()
    private Double longitude;

    @Column(length = 150)
    private String promoUrl;

    @Column(length = 200)
    private String description;

    @ElementCollection
    private List<String> mediaUrls = new ArrayList<>();

    @Column(length = 150)
    private String audioUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Language language;

    @ElementCollection
    private List<Double> audioTimestamps = new ArrayList<>();


    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionEntity> actions = new ArrayList<>();

    // Draft fields
    @Column(length = 30)
    private String titleDraft;

    @Column()
    private Double latitudeDraft;

    @Column()
    private Double longitudeDraft;

    @Column(length = 150)
    private String promoUrlDraft;

    @Column(length = 200)
    private String descriptionDraft;

    @ElementCollection
    private List<String> mediaUrlsDraft = new ArrayList<>();

    @Column(length = 150)
    private String audioUrlDraft;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Language languageDraft;

    @ElementCollection
    private List<Double> audioTimestampsDraft = new ArrayList<>();

    @OneToMany(mappedBy = "locationDraft", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionEntity> actionsDraft = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private boolean published = false;


    public Location(User user, String title, Language language) {
        this.user = user;
        this.titleDraft = title;
        this.languageDraft = language;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Location location = (Location) o;
        return getId() != null && Objects.equals(getId(), location.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}