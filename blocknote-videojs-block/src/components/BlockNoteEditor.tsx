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