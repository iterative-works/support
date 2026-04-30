# Poznámky k vydání: Dvojí publikování knihovny iw-support do e-BS Nexusu a GitHub Packages

**Issue:** SUPP-24
**Datum:** 2026-04-30

Knihovna `iw-support` se nově publikuje dvojím způsobem — do interního registru **e-BS Nexus** a souběžně do **GitHub Packages**. Toto rozšíření odděluje budoucí používání knihovny od konkrétní firemní infrastruktury, aniž by současní spotřebitelé v prostředí e-BS museli cokoli měnit.

Pro existující projekty v e-BS prostředí se nemění vůbec nic. Knihovnu i nadále stahují ze stejné adresy v e-BS Nexusu jako dosud, beze změny konfigurace resolveru a bez nutnosti přístupu k třetí straně. Tagované verze budou nadále spolehlivě k dispozici tam, kde je týmy zvyklé hledat.

Pro projekty mimo e-BS prostředí je nově k dispozici druhá cesta — GitHub Packages na adrese `https://maven.pkg.github.com/iterative-works/support`. Stačí v konfiguraci resolveru doplnit GitHub Personal Access Token s oprávněním ke čtení balíčků (`read:packages`). Tato varianta je vhodná všude tam, kde není přístup do interní sítě e-BS, ale projekt přesto chce stavět na sdílené podpůrné knihovně iw-support.

Vydávání nových verzí je nyní řízeno automaticky přes GitHub Actions. Při označení verze tagem ve tvaru `vX.Y.Z` proběhne automatický test a následné nahrání artefaktů do obou registrů zároveň. Pravidelné průběžné verze (snapshoty) z hlavní vývojové větve se publikují pouze do GitHub Packages; do e-BS Nexusu míří jen řádně otagovaná vydání, takže interní registr zůstává přehledný a obsahuje výhradně schválené verze.

Doprovodná dokumentace byla přepracována. Soubor `PUBLISHING.md` nově popisuje celý proces dvojí publikace, požadavky na přístupové údaje, postup pro místní publikaci během vývoje a postup obnovy při dílčí chybě nahrání. Soubor `README.md` byl rozšířen o jasné instrukce pro obě cesty spotřeby — ukázky závislostí pro Mill i sbt a samostatné bloky s konfigurací resolveru pro každý cílový registr. Pro místní vývoj zůstává příkaz `./mill __.publishLocal`, který nepotřebuje žádné přístupové údaje. Předchozí pomocný skript `publish.sh` byl odstraněn, protože sankcionovanou cestou k vydání je nyní GitHub Actions.

Aktuální verze knihovny po této změně je `0.1.14`. Pro úplnou aktivaci dvojí publikace je třeba, aby správce repozitáře nakonfiguroval v nastavení GitHub Actions dva přístupové údaje pro e-BS Nexus; přístup ke GitHub Packages probíhá automaticky bez další konfigurace.
