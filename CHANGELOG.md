# Changelog

## 1.8.1
- Every MyITMO request carries `Accept-Language` from
  `MyItmoConfiguration.getAcceptLanguage()` (default `ru`), so people search
  returns Cyrillic names instead of transliterations. An explicit header on a
  request wins.

## 1.8.0
- `api.bars`: BARS client with ITMO.ID login, SSO session login, external OIDC
  code exchange, silent renewal through `BarsCodeSupplier`, period selection,
  discipline and flow catalogs, student journal with marks and approvals.
- Javadoc links to Lombok getters fixed.

## 1.7.x
- Sport venue IDs documented as raw values; `building_id` nullable.
- Sport flow limits request; attendance nullability.

## 1.6.0
- Recordbook, control-event tree, sport semesters and scores as used by the
  ITMO.Widgets recordbook.
