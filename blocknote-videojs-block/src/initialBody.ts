import type { PartialBlock } from '@blocknote/core';

export const initialBody: PartialBlock[] = [
  {
    id: "4bbe148c-da40-428f-8c02-18aecab2e33d",
    type: "paragraph",
    props: { backgroundColor: "default", textColor: "default", textAlignment: "left" },
    content: [],
    children: [],
  },
  {
    id: "73d96bae-ee87-4bb3-b82f-4b8da9a6dbc5",
    type: "video",
    props: { url: "/hls/playlist.m3u8", name: "playlist.m3u8", caption: "", showPreview: true },
    children: [],
  },
  {
    id: "4f7490ce-3b18-45d2-b238-fe844591412b",
    type: "paragraph",
    props: { backgroundColor: "default", textColor: "default", textAlignment: "left" },
    content: [],
    children: [],
  },
];