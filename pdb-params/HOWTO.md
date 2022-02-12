# Vývoj

1) Otevřít dokument, otevřít replace dialog a nahradit data dle regexpu `((?:\d\.\d\.\d\.)|(?:\d\.\d))` za `ch$1`
    - matchnutá skupina + písmenko ch jako chapter. Důvod pro toto je ten, že když vložím do tabulky string například 1.2, chytrý excel/calc z toho automaticky udělá datum, a nepodařilo se mi přijít na řešení jak toto chování vypnout, dle fora libreoffice je to známý problém který nemá jednoduché řešení.
2) Zkopírovat obsah Ctrl + A, Ctrl + C
3) Otevřít nový excel/calc sheet, zkopírovat data z clipboardu
4) Uložit jako csv
5) Získat přístup k repozitáři a stáhnout release https://github.com/ondramastik/mdr-pdb-params-to-json-converter/releases/tag/v1.0 (pouze pro windows)
6) Spustit příkaz `MdrPdbParamsConvertor.exe C:\path\to\csv C:\path\to\generate\result.json`
7) Výsledný soubor by měl být zde: `C:\path\to\generate\result.json`
