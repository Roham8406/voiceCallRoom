// Simple frontend using WebSocket for signaling and WebRTC for audio
importScripts?; // noop for editors

const backendWs = () => {
    const loc = window.location;
    // adjust if backend runs on different host
    const wsProtocol = (loc.protocol === 'https:') ? 'wss' : 'ws';
    const port = 8080;
    return wsProtocol + '://' + loc.hostname + ':' + port + '/ws';
};

let ws;
let localStream;
let peers = {}; // userId -> RTCPeerConnection
let audioElements = {}; // userId -> audio element
let myUserId, myRoomId;

document.getElementById('joinBtn').addEventListener('click', async () => {
    myRoomId = document.getElementById('roomId').value.trim();
    myUserId = document.getElementById('userId').value.trim() || ('user' + Math.floor(Math.random()*1000));
    if (!myRoomId) return alert('enter a room id');
    await startLocalStream();
    connectWebSocket();
    document.getElementById('joinBtn').disabled = true;
    document.getElementById('leaveBtn').disabled = false;
});

document.getElementById('leaveBtn').addEventListener('click', () => {
    sendMessage({type:'leave', roomId: myRoomId, from: myUserId});
    for (const uid of Object.keys(peers)) {
        closePeer(uid);
    }
    if (ws) ws.close();
    if (localStream) {
        localStream.getTracks().forEach(t=>t.stop());
        localStream = null;
    }
    document.getElementById('participants').innerHTML = '';
    document.getElementById('joinBtn').disabled = false;
    document.getElementById('leaveBtn').disabled = true;
});

async function startLocalStream(){
    try {
        localStream = await navigator.mediaDevices.getUserMedia({audio:true});
        document.getElementById('localStatus').innerText = 'Local mic active';
    } catch(e){
        alert('Could not access microphone: ' + e.message);
    }
}

function connectWebSocket(){
    ws = new WebSocket(backendWs());
    ws.onopen = () => {
        sendMessage({type:'join', roomId: myRoomId, from: myUserId});
    };
    ws.onmessage = async (evt) => {
        const msg = JSON.parse(evt.data);
        const type = msg.type;
        if (type === 'participant-joined' || type === 'participant-left') {
            renderParticipants(msg.participants || []);
            return;
        }
        if (type === 'offer') {
            const from = msg.from;
            await handleOffer(from, msg.payload);
            return;
        }
        if (type === 'answer') {
            const from = msg.from;
            const pc = peers[from];
            if (pc) {
                await pc.setRemoteDescription(msg.payload);
            }
            return;
        }
        if (type === 'ice') {
            const from = msg.from;
            const pc = peers[from];
            if (pc && msg.payload) {
                try { await pc.addIceCandidate(msg.payload); } catch(e){}
            }
        }
    };
    ws.onclose = ()=> console.log('ws closed');
}

function sendMessage(obj){
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify(obj));
}

function renderParticipants(list){
    const container = document.getElementById('participants');
    container.innerHTML = '';
    for (const id of list) {
        if (id === myUserId) continue;
        const div = document.createElement('div');
        div.className = 'participant';
        div.id = 'p_' + id;
        const label = document.createElement('span');
        label.innerText = id;
        const muteBtn = document.createElement('button');
        muteBtn.innerText = 'Mute';
        muteBtn.onclick = ()=> toggleMute(id, muteBtn);
        div.appendChild(label);
        div.appendChild(muteBtn);
        const audio = document.createElement('audio');
        audio.id = 'audio_' + id;
        audio.autoplay = true;
        audio.controls = false;
        div.appendChild(audio);
        container.appendChild(div);

        // create peer connection to that user if not exists
        if (!peers[id]) {
            createPeerAsCaller(id);
        }
    }
}

function toggleMute(userId, btn){
    const audio = document.getElementById('audio_' + userId);
    if (!audio) return;
    audio.muted = !audio.muted;
    btn.innerText = audio.muted ? 'Unmute' : 'Mute';
}

function createPeerAsCaller(remoteId){
    const pc = new RTCPeerConnection({
        iceServers: [{urls: 'stun:stun.l.google.com:19302'}]
    });
    peers[remoteId] = pc;

    // add local tracks
    if (localStream) {
        for (const track of localStream.getTracks()) pc.addTrack(track, localStream);
    }

    pc.onicecandidate = (e) => {
        if (e.candidate) {
            sendMessage({type:'ice', roomId: myRoomId, from: myUserId, to: remoteId, payload: e.candidate});
        }
    };

    pc.ontrack = (e) => {
        const el = document.getElementById('audio_' + remoteId);
        if (el) {
            el.srcObject = e.streams[0];
        }
    };

    // create offer
    pc.createOffer().then(offer => pc.setLocalDescription(offer).then(()=> {
        sendMessage({type:'offer', roomId: myRoomId, from: myUserId, to: remoteId, payload: pc.localDescription});
    }));
}

async function handleOffer(remoteId, desc){
    // create peer if not exists
    if (!peers[remoteId]) {
        const pc = new RTCPeerConnection({iceServers:[{urls:'stun:stun.l.google.com:19302'}]});
        peers[remoteId] = pc;

        pc.onicecandidate = (e) => {
            if (e.candidate) {
                sendMessage({type:'ice', roomId: myRoomId, from: myUserId, to: remoteId, payload: e.candidate});
            }
        };

        pc.ontrack = (e) => {
            const el = document.getElementById('audio_' + remoteId);
            if (el) {
                el.srcObject = e.streams[0];
            }
        };

        if (localStream) {
            for (const track of localStream.getTracks()) pc.addTrack(track, localStream);
        }
    }
    const pc = peers[remoteId];
    await pc.setRemoteDescription(desc);
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);
    sendMessage({type:'answer', roomId: myRoomId, from: myUserId, to: remoteId, payload: pc.localDescription});
}

function closePeer(remoteId){
    const pc = peers[remoteId];
    if (pc) {
        try { pc.close(); } catch(e){}
        delete peers[remoteId];
    }
    const el = document.getElementById('p_' + remoteId);
    if (el) el.remove();
}
