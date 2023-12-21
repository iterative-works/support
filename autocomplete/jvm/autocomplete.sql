create table autocomplete
(
    collection varchar(128) not null,
    language   char(5)      not null,
    value      varchar(512) not null,
    label      varchar(768) not null,
    text       varchar(768) null,
    constraint autocomplete_pk
        primary key (collection, language, value)
);

create index autocomplete_collection_language_index
    on autocomplete (collection, language);
