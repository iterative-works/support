# Release Notes: Automatická kontrola kvality kódu

**Issue:** SUPP-9
**Datum:** 2026-01-30

Projekt nyní disponuje automatickými kontrolami kvality kódu, které pomáhají udržovat
konzistentní standardy napříč celou kódovou základnou. Tyto kontroly běží automaticky
při každém pull requestu a volitelně i lokálně před odesláním změn.

Při vytvoření pull requestu na GitHub se automaticky spustí čtyři kontroly: kompilace
všech modulů (JVM i JavaScript), ověření formátování kódu podle pravidel Scalafmt,
kontrola dodržování funkcionálních principů pomocí Scalafix (zákaz null, var, throw)
a spuštění všech jednotkových testů. Tyto kontroly běží paralelně tam, kde je to možné,
aby vývojáři dostali zpětnou vazbu co nejrychleji.

Pro lokální vývoj jsou k dispozici dva volitelné git hooky. Pre-commit hook kontroluje
formátování kódu před každým commitem a pre-push hook spouští testy před odesláním
změn na server. Instalace hooků je jednoduchá pomocí symbolických odkazů a je
zdokumentována v souboru CONTRIBUTING.md.

Nová dokumentace pro přispěvatele v souboru CONTRIBUTING.md obsahuje přehled všech
CI kontrol, návod na instalaci git hooků, příkazy pro lokální spuštění kontrol a
řešení nejčastějších problémů. Vývojáři si tak mohou ověřit kvalitu svého kódu
ještě před vytvořením pull requestu.
