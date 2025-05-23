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
@Table(name = "action_entity")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ActionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(length = 30)
    private String title;

    @Column(length = 300)
    private String description;

    @ElementCollection
    private List<String> answers = new ArrayList<>();

    @Column
    private Integer correctAnswerIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_draft_id")
    @ToString.Exclude
    private Location locationDraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    @ToString.Exclude
    private Location location;

}
