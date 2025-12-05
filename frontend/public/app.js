/*
Minimal WebRTC client compatible with the raw Java WebSocket server.
Encodes SDP and ICE candidates as base64 so they survive the server’s simple parser.
*/

let ws;
let localStream;
let pcs = {};            // remoteId -> RTCPeerConnection
let audioEls = {};       // remoteId -> HTMLAudioElement
let myUserId = '';
let myRoomId = '';

const hostEl = document.getElementById('host');
const portEl = document.getElementById('port');

document.getElementById('joinBtn').onclick = async () => {
  myRoomId = document.getElementById('roomId').value.trim();
  myUserId = document.getElementById('userId').value.trim() || ('user'+Math.floor(Math.random()*1000));

  if (!myRoomId) {
    alert('Enter a room ID');
    return;
  }

  // Start microphone
  try {
    localStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    setStatus('Microphone OK');
  } catch (e) {
    alert('Microphone error: ' + e.message);
    return;
  }

  connectWs();
  document.getElementById('joinBtn').disabled = true;
  document.getElementById('leaveBtn').disabled = false;
};

document.getElementById('leaveBtn').onclick = () => {
  send({ type:'leave', roomId: myRoomId, from: myUserId });

  for (const id in pcs) {
    try { pcs[id].close(); } catch(e){}
    removeParticipantUI(id);
    delete pcs[id];
  }

  if (ws) ws.close();
  if (localStream) {
    localStream.getTracks().forEach(t => t.stop());
    localStream = null;
  }

  document.getElementById('joinBtn').disabled = false;
  document.getElementById('leaveBtn').disabled = true;
  setStatus('Left room');
};

function wsUrl() {
  const host = hostEl.value || 'localhost';
  const port = portEl.value || '8080';
  const proto = location.protocol === 'https:' ? 'wss' : 'ws';
  return `${proto}://${host}:${port}/ws`;
}

function connectWs() {
  ws = new WebSocket(wsUrl());

  ws.onopen = () => {
    setStatus('Connected to signaling server');
    send({ type:'join', roomId: myRoomId, from: myUserId });
  };

  ws.onmessage = async (evt) => {
    let msg;
    try { msg = JSON.parse(evt.data); }
    catch { return console.warn('Bad message:', evt.data); }

    const type = msg.type;

    if (type === 'participant-joined' || type === 'user-joined') {
      const other = msg.user || msg.from;
      if (!other || other === myUserId) return;
      addParticipantUI(other);
      await createOfferFor(other);
      return;
    }

    if (type === 'offer' && msg.to === myUserId) {
      await handleRemoteOffer(msg.from, atob(msg.sdp));
      return;
    }

    if (type === 'answer' && msg.to === myUserId) {
      const pc = pcs[msg.from];
      if (pc) await pc.setRemoteDescription({ type:'answer', sdp: atob(msg.sdp) });
      return;
    }

    if (type === 'ice' && msg.to === myUserId) {
      try {
        const candObj = JSON.parse(atob(msg.candidate));
        const pc = pcs[msg.from];
        if (pc) await pc.addIceCandidate(candObj);
      } catch(e) {
        console.warn('ICE decode failed', e);
      }
      return;
    }
  };

  ws.onclose = () => setStatus('WS disconnected');
  ws.onerror = () => setStatus('WS error');
}

function send(o) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return;
  ws.send(JSON.stringify(o));
}

/* UI helpers */
function setStatus(s) {
  document.getElementById('status').innerText = s;
}

function addParticipantUI(id) {
  if (document.getElementById('p_' + id)) return;

  const div = document.createElement('div');
  div.className = 'participant';
  div.id = 'p_' + id;

  const label = document.createElement('span');
  label.innerText = id;

  const muteBtn = document.createElement('button');
  muteBtn.innerText = 'Mute';

  muteBtn.onclick = () => {
    const a = audioEls[id];
    if (!a) return;
    a.muted = !a.muted;
    muteBtn.innerText = a.muted ? 'Unmute' : 'Mute';
  };

  const audio = document.createElement('audio');
  audio.id = 'audio_' + id;
  audio.autoplay = true;

  div.append(label, muteBtn, audio);
  document.getElementById('participants').appendChild(div);

  audioEls[id] = audio;
}

function removeParticipantUI(id) {
  const el = document.getElementById('p_' + id);
  if (el) el.remove();
  delete audioEls[id];
}

/* WebRTC */
function getOrCreatePc(remoteId) {
  if (pcs[remoteId]) return pcs[remoteId];

  const pc = new RTCPeerConnection({
    iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
  });

  if (localStream) {
    for (const t of localStream.getTracks())
      pc.addTrack(t, localStream);
  }

  pc.onicecandidate = (e) => {
    if (e.candidate) {
      send({
        type: 'ice',
        roomId: myRoomId,
        from: myUserId,
        to: remoteId,
        candidate: btoa(JSON.stringify(e.candidate))
      });
    }
  };

  pc.ontrack = (e) => {
    addParticipantUI(remoteId);
    audioEls[remoteId].srcObject = e.streams[0];
  };

  pc.onconnectionstatechange = () => {
    if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
      removeParticipantUI(remoteId);
      delete pcs[remoteId];
    }
  };

  pcs[remoteId] = pc;
  return pc;
}

async function createOfferFor(remoteId) {
  addParticipantUI(remoteId);
  const pc = getOrCreatePc(remoteId);

  const offer = await pc.createOffer();
  await pc.setLocalDescription(offer);

  send({
    type: 'offer',
    roomId: myRoomId,
    from: myUserId,
    to: remoteId,
    sdp: btoa(offer.sdp)
  });
}

async function handleRemoteOffer(fromId, sdp) {
  addParticipantUI(fromId);
  const pc = getOrCreatePc(fromId);

  await pc.setRemoteDescription({ type:'offer', sdp });
  const answer = await pc.createAnswer();
  await pc.setLocalDescription(answer);

  send({
    type: 'answer',
    roomId: myRoomId,
    from: myUserId,
    to: fromId,
    sdp: btoa(answer.sdp)
  });
}
