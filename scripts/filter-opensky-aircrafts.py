import csv
import re

opensky_aircrafts = []
with open('data/opensky_aircrafts.csv', 'r', encoding='ISO-8859-1') as f:
  reader = csv.reader(f)
  for row in reader:
    opensky_aircrafts.append(row)

interesting_opensky_aircrafts = []
for row in opensky_aircrafts:
  def interesting(column):
    if re.match('.*(patrol|police|policia|sheriff|state of|highway|law enforce).*', column, re.IGNORECASE):
      if not re.match('.*(PATROL).*', column):
        if not re.match('.*(civil air patrol|forest patrol ltd|bearhawk patrol|llc|patroller|patrol inc|patrols inc).*', column, re.IGNORECASE):
          return True
    return False

  if row[0][:1] == 'a' and (interesting(row[9]) or interesting(row[13])):
    interesting_opensky_aircrafts.append([row[0],row[3],row[8],row[13]])

with open('data/interesting_opensky_aircrafts.csv', 'w') as f:
  writer = csv.writer(f)
  for row in interesting_opensky_aircrafts:
    writer.writerow(row)

