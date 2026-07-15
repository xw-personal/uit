from ultralytics import YOLO
from pathlib import Path
from fastapi import FastAPI, File,UploadFile
from uvicorn import run
import json
import cv2, numpy as np


app = FastAPI()
model_path = Path(__file__).parent / 'best.pt'
model = YOLO(model_path)


@app.post("/analyze")
async def analyze(file: UploadFile = File(...)):
    contents = await file.read()
    nparr = np.frombuffer(contents,np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    result = model.predict(img,imgsz=640)[0]
    boxes = result.boxes
    img_height, img_width = result.orig_shape
    if boxes is not None:
        elements = []
        for box in boxes:
            # 获取坐标 (左上角 x, y 和 右下角 x, y)
            x1,y1,x2,y2 = box.xyxy[0].tolist()
            conf = float(box.conf[0])
            cls_id = int(box.cls[0])
            cls_name = result.names[cls_id]
            center_x = (x1+x2) / 2
            center_y = (y1+y2) /2
            elements.append({
                "type": cls_name,
                "confidence": round(conf,2),
                "bbox": [round(x1,2),round(y1,2),round(x2,2),round(y2,2)],
                "center": [round(center_x),round(center_y)]
            })
    analyze_input = {
        "page_size": {"width": img_width, "height": img_height},
        "elements_count": len(elements),
        "elements": elements
    }
    return json.dumps(analyze_input, indent=2)
    

if __name__ == "__main__":
    run(
        "main:app",                          # FastAPI 应用实例
        host="0.0.0.0",               # 监听地址，0.0.0.0 允许外部访问
        port=8000,                    # 端口
        reload=True,                  # 代码变更时自动重启（开发环境）
        # workers=1,                    # 工作进程数（生产环境）
        log_level="info"              # 日志级别
    )
