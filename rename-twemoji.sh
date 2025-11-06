#!/bin/bash

EMOTE_DIRECTORY="./twemoji/v/latest/72x72"
EXTENSION=".png"

#**-20e3 **-fe0f-20e3
#2763 2763-fe0f
#2764 2764-fe0f
#1f17* 1f17*-fe0f
#13f32[1-c]
#1f39[6-f]
#1f3d[4-f]


# note: could try adding -fe0f to all 1f5** with no additional modifiers?

#1f58[7-d]
#1f5[a5-fa]


seq 0x24c2 0x25fb | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done


seq 0x20 0x39 | while read n; do
  hex="$(printf '%02x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}-20e3${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f-20e3${EXTENSION}"
done

seq 0x1f170 0x1f17f | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f321 0x1f32c | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f396 0x1f39f | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f3d4 0x1f3df | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f587 0x1f58d | while read n; do
  hex="$(printf '%04x' "$n")"
  echo $hex
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f5a5 0x1f5fa | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x1f6e0 0x1f6e9 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x2139 0x21aa | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x23f8 0x23fc | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x25ff 0x2611 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x261d 0x2642 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x265f 0x267e | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x2694 0x26a0 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x270d 0x2721 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x3030 0x3299 | while read n; do
  hex="$(printf '%04x' "$n")"
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

seq 0x2934 0x2b07 | while read n; do
  hex="$(printf '%04x' "$n")"
  #echo $hex
  mv "${EMOTE_DIRECTORY}/${hex}${EXTENSION}" "${EMOTE_DIRECTORY}/${hex}-fe0f${EXTENSION}"
done

#1f6c[d-f]
#1f6f[0,3]

printf "%s\n" "${arr[@]}"
# single character renames
arr=("25fc" "1f37d" "1f43f" "1f6cb" "1f6cd" "1f6ce" "2763" "2764" "1f6cf" "1f6f0" "1f6f1" "1f6f1" "1f6f2" "1f6f3" "1f590" "2328" "23cf" "23f1" "23f2" "23ed" "23ee" "23ef" "1f202" "1f237" "1f336" "1f3cc" "1f3cb" "1f3cd" "1f3ce" "1f3f3" "1f3f5" "1f3f7" "1f441" "1f4fd" "1f549" "1f54a" "1f56f" "1f570" "1f573" "1f574" "1f575" "1f576" "1f577" "1f578" "1f579" "203c" "2049" "2618" "2692" "26b0" "26b1" "26c8" "26cf" "26d1" "26d3" "26e9" "26f0" "1f3d6" "26f4" "26f7" "26f8" "26f9" "2702" "2708" "2709" "270c" "2733" "2734" "2744" "2747" "27a1")
for b in ${arr[@]}; do
  #echo "Hi! ${b}"
  mv "${EMOTE_DIRECTORY}/${b}${EXTENSION}" "${EMOTE_DIRECTORY}/${b}-fe0f${EXTENSION}"
done