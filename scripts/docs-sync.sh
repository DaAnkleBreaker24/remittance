#!/usr/bin/env bash
# Regenerates the Markdown copies in docs/ from the Word and Excel files (the reviewed sources).
# Needs: pandoc (installer from https://pandoc.org/installing.html) and python3 with openpyxl (pip install openpyxl).
set -euo pipefail
cd "$(dirname "$0")/../docs"

pandoc Remittance_Platform_Technical_Design.docx -t gfm --wrap=none -o architecture.md
pandoc Remittance_Platform_Functional_Design.docx -t gfm --wrap=none -o functional-design.md

python3 - <<'PY'
import re, openpyxl
# architecture.md: point images at the PNGs in docs/, add a header
s = open('architecture.md').read()
it = iter(['d1_logical.png', 'd4_flow.png', 'd3_ports.png', 'd2_deployment.png'])
s = re.sub(r'<img src="media/[^"]*" title="([^"]*)"[^>]*/>', lambda m: f'![{m.group(1)}]({next(it)})', s)
s = s.replace('\\.', '.')
open('architecture.md', 'w').write("> Generated from Remittance_Platform_Technical_Design.docx by scripts/docs-sync.sh. Edit the Word file, then re-run.\n\n" + s)

s = open('functional-design.md').read().replace('\\.', '.')
open('functional-design.md', 'w').write("> Generated from Remittance_Platform_Functional_Design.docx by scripts/docs-sync.sh. Edit the Word file, then re-run.\n\n" + s)

wb = openpyxl.load_workbook('Remittance_Backlog_Epics_UserStories.xlsx', data_only=True)
rows = list(wb['User Stories'].iter_rows(values_only=True))[1:]
out = ["# Backlog (generated from Remittance_Backlog_Epics_UserStories.xlsx by scripts/docs-sync.sh)", "",
       "The Excel workbook is the source of truth for status and owners. This file is a read-only copy so Claude Code can see the stories.", "",
       "Work the stories in the coding order given in CLAUDE.md.", ""]
cur = None
for r in rows:
    sid, eid, epic, title, story, ac, prio, rel, pts = r[:9]
    if not sid:
        continue
    if eid != cur:
        cur = eid
        out += ["", f"## {eid} {epic}", ""]
    out.append(f"### {sid} {title}  ({prio}, {rel}, {pts} pts)")
    out.append(story or "")
    if ac:
        out.append("Acceptance:")
        out += ["  " + l.strip() for l in str(ac).splitlines() if l.strip()]
    if r[12]:
        out.append(f"Note: {r[12]}")
    out.append("")
open('backlog.md', 'w').write("\n".join(out))
print(f"synced: architecture.md, functional-design.md, backlog.md ({len([r for r in rows if r[0]])} stories)")
PY
