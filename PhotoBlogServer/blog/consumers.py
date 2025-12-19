import json
from channels.generic.websocket import AsyncWebsocketConsumer

class NotificationConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        # 모든 클라이언트가 'notifications' 그룹에 참여
        self.group_name = 'notifications'
        
        await self.channel_layer.group_add(
            self.group_name,
            self.channel_name
        )
        
        await self.accept()
        print(f"WebSocket 연결됨: {self.channel_name}")
        
        # Edge 연결 상태 전송
        await self.send(text_data=json.dumps({
            'type': 'edge_status',
            'status': 'connected',
            'message': '카메라 연결됨'
        }))

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(
            self.group_name,
            self.channel_name
        )
        print(f"WebSocket 연결 해제: {self.channel_name}")

    async def receive(self, text_data):
        # 클라이언트로부터 메시지 수신 (필요시)
        pass

    # 그룹 메시지 핸들러
    async def detection_alert(self, event):
        # YOLO에서 보낸 알림을 클라이언트로 전달
        await self.send(text_data=json.dumps(event['message']))

    async def object_count(self, event):
        # 객체 카운트 업데이트
        await self.send(text_data=json.dumps(event['message']))
    
    async def edge_status(self, event):
        # Edge 연결 상태 변경
        await self.send(text_data=json.dumps(event['message']))