spesidaler
============
Lagrer tilleggsinformasjon som hender underveis i sykefraværet. Og deler dette med sykepengevedtaksløsningen, spleis, slik at den kan beregne vedtaket riktig.

Per nå lagres:
- nye inntekter underveis i sykefraværet som sykepengevedtaksløsningen ikke har opprettet en vedtaksperiode for

# Behov og løsning
```json
{
  "@behov": [
    "InntekterForBeregning" 
  ],
  "fødselsnummer": "11111111111",
  "InntekterForBeregning": {
    "fom": "2018-01-01",
    "tom": "2018-01-31"
  },
  "@løsning": {
    "InntekterForBeregning": {
      "inntekter": [
        {
          "fom": "2018-01-01",
          "tom": "2018-01-14",
          "kilde": "999999999",
          "beløp": {
            "ører": 10000,
            "oppløsning": "Daglig"
          }
        },
        {
          "fom": "2018-01-15",
          "tom": "2018-01-31",
          "kilde": "999999999",
          "beløp": {
            "ører": 20000,
            "oppløsning": "Månedlig"
          }
        },
        {
          "fom": "2018-01-20",
          "tom": "2018-01-25",
          "kilde": "111111111",
          "beløp": {
            "ører": 40000,
            "oppløsning": "Årlig"
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
