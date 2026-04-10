# BlockNote Video.js v10 custom block

A custom block that replaces the default video player with Video.js when inserting a video in BlockNote.

![demo](/videojs.gif)

## Version
```
React 19+ (required for Video.js v10)
@blocknote/* ^0.45.0
@videojs/react ^10.0.0-beta.15
```

## Code

### `src/components/VideoPlayer.tsx`

```
import { createReactBlockSpec, FileBlockWrapper } from "@blocknote/react";
import { MdVideocam } from "react-icons/md";
import { VideoPlayer } from "../VideoPlayer";

const VideoBlockComponent = (props: any) => {
  if (props.block.props.url) {
    return (
      <VideoPlayer src={props.block.props.url} />
    );
  }
  return (
    <FileBlockWrapper {...props} buttonIcon={<MdVideocam size={24} />} />
  );
};

export const createVideoBlock = createReactBlockSpec(
  {
    type: "video",
    propSchema: {
      url: { default: "" },
      name: { default: "" },
      caption: { default: "" },
      showPreview: { default: true },
      previewWidth: { default: undefined, type: "number" as const },
    },
    content: "none",
  },
  {
    meta: { fileBlockAccept: ["video/*"], selectable: false },
    render: VideoBlockComponent,
  }
);

```

### `src/components/blocks/VideoBlock.tsx`

```
import { createReactBlockSpec, FileBlockWrapper } from "@blocknote/react";
import { MdVideocam } from "react-icons/md";
import { VideoPlayer } from "../VideoPlayer";

const VideoBlockComponent = (props: any) => {
  if (props.block.props.url) {
    return (
      <VideoPlayer src={props.block.props.url} />
    );
  }
  return (
    <FileBlockWrapper {...props} buttonIcon={<MdVideocam size={24} />} />
  );
};

export const createVideoBlock = createReactBlockSpec(
  {
    type: "video",
    propSchema: {
      url: { default: "" },
      name: { default: "" },
      caption: { default: "" },
      showPreview: { default: true },
      previewWidth: { default: undefined, type: "number" as const },
    },
    content: "none",
  },
  {
    meta: { fileBlockAccept: ["video/*"], selectable: false },
    render: VideoBlockComponent,
  }
);
```

### `src/components/BlockNoteEditor.tsx`

```
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteSchema, defaultBlockSpecs } from "@blocknote/core";
import type { PartialBlock } from "@blocknote/core";
import { BlockNoteView } from "@blocknote/mantine";
import { codeBlockOptions } from "@blocknote/code-block";
import { createCodeBlockSpec } from "@blocknote/core";
import { ko } from "@blocknote/core/locales";

import { createVideoBlock } from "./blocks/VideoBlock";
import "@blocknote/mantine/style.css";
import "@blocknote/core/fonts/inter.css";

const schema = BlockNoteSchema.create({
  blockSpecs: {
    ...defaultBlockSpecs,
    codeBlock: createCodeBlockSpec(codeBlockOptions),
    video: createVideoBlock(),
  },
});

interface BlockNoteEditorProps {
  editable?: boolean;
  body?: PartialBlock[];
  onChange?: (blocks: PartialBlock[]) => void;
  uploadFile?: (file: File) => Promise<string>;
}

const BlockNoteEditor = ({
  editable = true,
  body,
  onChange,
  uploadFile,
}: BlockNoteEditorProps) => {
  const editor = useCreateBlockNote({
    initialContent: Array.isArray(body) ? body : undefined,
    schema,
    dictionary: ko,
    uploadFile,
  });

  const handleOnChange = () => {
    if (onChange) {
      onChange(editor.document);
    }
  };

  return (
    <BlockNoteView
      editor={editor}
      theme="light"
      editable={editable}
      onChange={handleOnChange}
    />
  );
};

export default BlockNoteEditor;
```
