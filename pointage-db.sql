--
-- PostgreSQL database dump
--

\restrict q813oXQc9LK5HjZomtaB6zm7lqIKUJLITdaE3XLh4EhaTOT3saKi2t8bgwKkySI

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

COMMENT ON TABLE public.pointage IS 'Enregistrement des pointages (présences/retards)';


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
-- Name: pointage_id_pointage_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pointage_id_pointage_seq', 233, true);


--
-- Name: sanction_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sanction_id_seq', 4558, true);


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

\unrestrict q813oXQc9LK5HjZomtaB6zm7lqIKUJLITdaE3XLh4EhaTOT3saKi2t8bgwKkySI

