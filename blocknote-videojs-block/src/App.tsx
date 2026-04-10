import { useState } from 'react';
import type { PartialBlock } from '@blocknote/core';
import BlockNoteEditor from './components/BlockNoteEditor';
import { initialBody } from './initialBody';

async function uploadFile(_file: File): Promise<string> {
  return '/hls/playlist.m3u8'; // or return '/video.mp4';
}

function App() {
  const [body, setBody] = useState<PartialBlock[] | undefined>(initialBody);

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      padding: '48px 16px',
      minHeight: '100vh',
      backgroundColor: '#f5f5f5',
    }}>
      <div style={{ width: '100%', maxWidth: '800px' }}>
        <BlockNoteEditor
          body={body}
          onChange={setBody}
          uploadFile={uploadFile}
        />
      </div>

      <div style={{
        width: '100%',
        maxWidth: '800px',
        marginTop: '32px',
      }}>
        <p style={{ fontSize: '12px', color: '#888', marginBottom: '8px' }}>document JSON</p>
        <pre style={{
          backgroundColor: '#1e1e1e',
          color: '#d4d4d4',
          padding: '16px',
          borderRadius: '8px',
          fontSize: '12px',
          overflow: 'auto',
          maxHeight: '400px',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-all',
        }}>
          {JSON.stringify(body, null, 2)}
        </pre>
      </div>
      <br/>
      <p style={{ fontSize: '14px' }}>Video by <a href="https://pixabay.com/users/kost9n4-2105326/?utm_source=link-attribution&utm_medium=referral&utm_campaign=video&utm_content=262189">Konstantin Kolosov</a> from <a href="https://pixabay.com//?utm_source=link-attribution&utm_medium=referral&utm_campaign=video&utm_content=262189">Pixabay</a></p>
    </div>
  );
}

export default App;