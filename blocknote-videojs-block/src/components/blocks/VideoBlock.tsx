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
