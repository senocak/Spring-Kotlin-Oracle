create table roles(
    created_at timestamp(6),
    updated_at timestamp(6),
    id         raw(16) not null primary key,
    name       varchar2(255) constraint roles_name_check check (name in ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_MIDWIFE', 'ROLE_NURSE', 'ROLE_THERAPIST'))
);
create table users(
    created_at  timestamp(6),
    updated_at  timestamp(6),
    id          raw(16) not null primary key,
    last_name   varchar2(50),
    name        varchar2(50)  not null,
    email       varchar2(100) not null unique,
    password    varchar2(255) not null
);
create table user_roles(
    role_id raw(16) not null constraint fk_user_roles_role_id references roles(id),
    user_id raw(16) not null constraint fk_user_roles_user_id references users(id),
    constraint uk_user_roles_unique_user_id_role_id unique (user_id, role_id)
);