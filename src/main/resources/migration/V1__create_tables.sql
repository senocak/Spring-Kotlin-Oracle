create table public.roles(
    created_at timestamp(6),
    updated_at timestamp(6),
    id         uuid not null primary key,
    name       varchar(255) constraint roles_name_check check ((name)::text = ANY ((ARRAY ['ROLE_USER'::character varying, 'ROLE_ADMIN'::character varying, 'ROLE_MIDWIFE'::character varying, 'ROLE_NURSE'::character varying, 'ROLE_THERAPIST'::character varying])::text[]))
);
create table public.users(
    created_at              timestamp(6),
    updated_at              timestamp(6),
    id                      uuid    not null primary key,
    last_name               varchar(50)  not null,
    name                    varchar(50)  not null,
    email                   varchar(100) not null unique,
    password                varchar(255) not null
);
create table public.user_roles(
    role_id uuid not null constraint fk_user_roles_role_id references public.roles,
    user_id uuid not null constraint fk_user_roles_user_id references public.users,
    constraint uk_user_roles_unique_user_id_role_id unique (user_id, role_id)
);