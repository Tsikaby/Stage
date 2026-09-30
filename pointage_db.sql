--
-- PostgreSQL database dump
--

\restrict Fg1N3Vk6JLacqoS8Sn1LxGKjif2ekDjDSl0ha1zRr1ujJyqYBn4Amb0yz06LDaH

-- Dumped from database version 17.6
-- Dumped by pg_dump version 17.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: examen; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.examen (
    id_examen bigint NOT NULL,
    date_examen date NOT NULL,
    heure_debut timestamp with time zone NOT NULL,
    heure_fin timestamp with time zone NOT NULL,
    duree numeric(30,2) NOT NULL,
    id_matiere integer NOT NULL,
    id_niveau integer NOT NULL,
    numero_salle character varying(30) NOT NULL
);


ALTER TABLE public.examen OWNER TO postgres;

--
-- Name: TABLE examen; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.examen IS 'Catalogue des examens avec dates et sessions';


--
-- Name: matiere; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.matiere (
    id_matiere integer NOT NULL,
    nom_matiere character varying(20) NOT NULL
);


ALTER TABLE public.matiere OWNER TO postgres;

--
-- Name: TABLE matiere; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.matiere IS 'Catalogue des matiŠres';


--
-- Name: niveau; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.niveau (
    id_niveau integer NOT NULL,
    code_niveau character varying(20) NOT NULL
);


ALTER TABLE public.niveau OWNER TO postgres;

--
-- Name: TABLE niveau; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.niveau IS 'Catalogue des niveaux acad‚miques';


--
-- Name: planning_surveillance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.planning_surveillance (
    id_planning bigint NOT NULL,
    id_examen integer NOT NULL,
    numero_salle character varying(30) NOT NULL,
    id_surveillant integer NOT NULL,
    date_examen character varying(50) NOT NULL,
    heure_debut character varying(20) NOT NULL,
    heure_fin character varying(20) NOT NULL
);


ALTER TABLE public.planning_surveillance OWNER TO postgres;

--
-- Name: TABLE planning_surveillance; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.planning_surveillance IS 'Planification des surveillances';


--
-- Name: pointage; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.pointage (
    id_pointage integer NOT NULL,
    heure_pointage timestamp with time zone NOT NULL,
    retard boolean NOT NULL,
    numero_salle character varying(20),
    id_surveillant integer NOT NULL
);


ALTER TABLE public.pointage OWNER TO postgres;

--
-- Name: TABLE pointage; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.pointage IS 'Enregistrement des pointages (pr‚sences/retards)';


--
-- Name: pointage_id_pointage_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.pointage ALTER COLUMN id_pointage ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.pointage_id_pointage_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: salle; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.salle (
    numero_salle character varying(30) NOT NULL,
    capacite_max integer NOT NULL,
    nbr_surveillant integer NOT NULL
);


ALTER TABLE public.salle OWNER TO postgres;

--
-- Name: TABLE salle; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.salle IS 'Informations sur les salles d''examen';


--
-- Name: sanction; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sanction (
    id bigint NOT NULL,
    id_surveillant bigint NOT NULL,
    type text NOT NULL,
    date_examen date NOT NULL,
    nom_surveillant text,
    numero_salle text,
    date_creation time without time zone,
    session character varying(20),
    CONSTRAINT sanction_type_check CHECK ((type = ANY (ARRAY['RETARD'::text, 'ABSENCE'::text])))
);


ALTER TABLE public.sanction OWNER TO postgres;

--
-- Name: TABLE sanction; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sanction IS 'Suivi des sanctions par surveillant';


--
-- Name: sanction_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sanction_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sanction_id_seq OWNER TO postgres;

--
-- Name: sanction_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sanction_id_seq OWNED BY public.sanction.id;


--
-- Name: surveillant; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.surveillant (
    id_surveillant bigint NOT NULL,
    nom_surveillant character varying(30) NOT NULL,
    groupe_surveillant character varying(50) NOT NULL,
    contact character varying(50) NOT NULL,
    numero_salle character varying(30) NOT NULL
);


ALTER TABLE public.surveillant OWNER TO postgres;

--
-- Name: TABLE surveillant; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.surveillant IS 'Informations sur les surveillants d''examens';


--
-- Name: utilisateurs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.utilisateurs (
    username character varying(255) NOT NULL,
    mdp character varying(255) NOT NULL,
    log boolean DEFAULT false,
    id bigint NOT NULL,
    role character varying(20) DEFAULT 'user'::character varying,
    approved boolean DEFAULT false
);


ALTER TABLE public.utilisateurs OWNER TO postgres;

--
-- Name: TABLE utilisateurs; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.utilisateurs IS 'Table des utilisateurs du systŠme avec r“les et approbations';


--
-- Name: utilisateurs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.utilisateurs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.utilisateurs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sanction id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sanction ALTER COLUMN id SET DEFAULT nextval('public.sanction_id_seq'::regclass);


--
-- Data for Name: examen; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.examen (id_examen, date_examen, heure_debut, heure_fin, duree, id_matiere, id_niveau, numero_salle) FROM stdin;
1	2026-09-30	2026-09-30 08:00:00+03	2026-09-30 10:00:00+03	2.00	1	1	Salle 101
2	2026-09-30	2026-09-30 10:00:00+03	2026-09-30 12:00:00+03	2.00	2	1	Salle 101
3	2026-09-30	2026-09-30 14:00:00+03	2026-09-30 16:00:00+03	2.00	3	2	Salle 102
4	2026-09-30	2026-09-30 14:00:00+03	2026-09-30 17:00:00+03	3.00	4	2	Labo Info 1
\.


--
-- Data for Name: matiere; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.matiere (id_matiere, nom_matiere) FROM stdin;
1	Algorithmique
2	Base de donn‚es
3	R‚seaux SystŠmes
4	D‚veloppement Web
\.


--
-- Data for Name: niveau; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.niveau (id_niveau, code_niveau) FROM stdin;
1	L1 Informatique
2	L2 Informatique
\.


--
-- Data for Name: planning_surveillance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.planning_surveillance (id_planning, id_examen, numero_salle, id_surveillant, date_examen, heure_debut, heure_fin) FROM stdin;
101	1	Salle 101	10	2026-09-30	08:00	10:00
102	1	Salle 101	11	2026-09-30	08:00	10:00
103	2	Salle 101	10	2026-09-30	10:00	12:00
104	2	Salle 101	11	2026-09-30	10:00	12:00
105	3	Salle 102	12	2026-09-30	14:00	16:00
106	4	Labo Info 1	13	2026-09-30	14:00	17:00
\.


--
-- Data for Name: pointage; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.pointage (id_pointage, heure_pointage, retard, numero_salle, id_surveillant) FROM stdin;
229	2026-09-30 13:22:32+03	f	Salle 102	12
\.


--
-- Data for Name: salle; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.salle (numero_salle, capacite_max, nbr_surveillant) FROM stdin;
Salle 101	50	2
Salle 102	40	1
Labo Info 1	30	1
\.


--
-- Data for Name: sanction; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sanction (id, id_surveillant, type, date_examen, nom_surveillant, numero_salle, date_creation, session) FROM stdin;
4144	10	ABSENCE	2026-09-30	Jean Dupont	Salle 101	13:17:39	Matin
4145	11	ABSENCE	2026-09-30	Alice Ranoro	Salle 101	13:17:39	Matin
\.


--
-- Data for Name: surveillant; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.surveillant (id_surveillant, nom_surveillant, groupe_surveillant, contact, numero_salle) FROM stdin;
10	Jean Dupont	Enseignants Permanents	+261 34 00 111 01	Salle 101
11	Alice Ranoro	Vacataires	+261 32 00 111 02	Salle 101
12	Michel Rakoto	Doctorants	+261 33 00 111 03	Salle 102
13	Rova Sahondra	Enseignants Permanents	+261 34 00 111 04	Labo Info 1
\.


--
-- Data for Name: utilisateurs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.utilisateurs (username, mdp, log, id, role, approved) FROM stdin;
surveillant1	surv123	f	9	surveillant	t
surveillant2	surv456	f	10	surveillant	t
user1	user123	f	11	user	t
user2	user456	f	12	user	t
solofo	kibaiz	f	13	user	f
admin	admin123	t	1	admin	t
\.


--
-- Name: pointage_id_pointage_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pointage_id_pointage_seq', 229, true);


--
-- Name: sanction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sanction_id_seq', 4147, true);


--
-- Name: utilisateurs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.utilisateurs_id_seq', 13, true);


--
-- Name: examen examen_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examen
    ADD CONSTRAINT examen_pkey PRIMARY KEY (id_examen);


--
-- Name: matiere matiere_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.matiere
    ADD CONSTRAINT matiere_pkey PRIMARY KEY (id_matiere);


--
-- Name: niveau niveau_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.niveau
    ADD CONSTRAINT niveau_pkey PRIMARY KEY (id_niveau);


--
-- Name: planning_surveillance planning_surveillance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.planning_surveillance
    ADD CONSTRAINT planning_surveillance_pkey PRIMARY KEY (id_planning);


--
-- Name: pointage pointage_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pointage
    ADD CONSTRAINT pointage_pkey PRIMARY KEY (id_pointage);


--
-- Name: salle salle_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.salle
    ADD CONSTRAINT salle_pkey PRIMARY KEY (numero_salle);


--
-- Name: sanction sanction_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sanction
    ADD CONSTRAINT sanction_pkey PRIMARY KEY (id);


--
-- Name: surveillant surveillant_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.surveillant
    ADD CONSTRAINT surveillant_pkey PRIMARY KEY (id_surveillant);


--
-- Name: utilisateurs utilisateurs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.utilisateurs
    ADD CONSTRAINT utilisateurs_pkey PRIMARY KEY (username);


--
-- Name: idx_examen_date_debut; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_examen_date_debut ON public.examen USING btree (date_examen, heure_debut);


--
-- Name: idx_planning_surv; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_planning_surv ON public.planning_surveillance USING btree (id_surveillant);


--
-- Name: idx_pointage_surveillant_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_pointage_surveillant_time ON public.pointage USING btree (id_surveillant, heure_pointage DESC);


--
-- Name: sanction_unq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX sanction_unq ON public.sanction USING btree (id_surveillant, type, date_examen, numero_salle);


--
-- Name: examen examen_id_matiere_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examen
    ADD CONSTRAINT examen_id_matiere_fkey FOREIGN KEY (id_matiere) REFERENCES public.matiere(id_matiere);


--
-- Name: examen examen_id_niveau_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examen
    ADD CONSTRAINT examen_id_niveau_fkey FOREIGN KEY (id_niveau) REFERENCES public.niveau(id_niveau);


--
-- Name: examen examen_numero_salle_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.examen
    ADD CONSTRAINT examen_numero_salle_fkey FOREIGN KEY (numero_salle) REFERENCES public.salle(numero_salle);


--
-- Name: planning_surveillance planning_surveillance_id_examen_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.planning_surveillance
    ADD CONSTRAINT planning_surveillance_id_examen_fkey FOREIGN KEY (id_examen) REFERENCES public.examen(id_examen);


--
-- Name: planning_surveillance planning_surveillance_id_surveillant_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.planning_surveillance
    ADD CONSTRAINT planning_surveillance_id_surveillant_fkey FOREIGN KEY (id_surveillant) REFERENCES public.surveillant(id_surveillant);


--
-- Name: planning_surveillance planning_surveillance_numero_salle_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.planning_surveillance
    ADD CONSTRAINT planning_surveillance_numero_salle_fkey FOREIGN KEY (numero_salle) REFERENCES public.salle(numero_salle);


--
-- Name: pointage pointage_id_surveillant_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.pointage
    ADD CONSTRAINT pointage_id_surveillant_fkey FOREIGN KEY (id_surveillant) REFERENCES public.surveillant(id_surveillant);


--
-- PostgreSQL database dump complete
--

\unrestrict Fg1N3Vk6JLacqoS8Sn1LxGKjif2ekDjDSl0ha1zRr1ujJyqYBn4Amb0yz06LDaH

