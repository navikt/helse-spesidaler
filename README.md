spesidaler
============
Lagrer tilleggsinformasjon som hender underveis i sykefraværet. Og deler dette med sykepengevedtaksløsningen, spleis, slik at den kan beregne vedtaket riktig.

Per nå lagres:
- nye inntekter underveis i sykefraværet som sykepengevedtaksløsningen ikke har opprettet en vedtaksperiode for

# Behov og løsning
```json
{
  "@behov": [
    "Inntekter" 
  ],
  "fødselsnummer": "11111111111",
  "Inntekter": {
    "inntekterFom": "2018-01-01",
    "inntekterTom": "2018-01-31"
  },
  "@løsning": {
    "Inntekter": {
      "inntekter": [
        {
          "fom": "2018-01-01",
          "tom": "2018-01-31",
          "kilde": "999999999",
          "beløp": {
            "ører": 10000,
            "oppløsning": "Daglig"
          }
        }
      ]
    }
  }
}
```

# Henvendelser
Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte
Interne henvendelser kan sendes via Slack i kanalen #team-bømlo-værsågod.
