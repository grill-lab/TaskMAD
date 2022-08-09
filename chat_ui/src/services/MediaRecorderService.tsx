interface IMediaRecorderService {

    pauseMedia: () => void;
    resumeMedia: () => void;
    startMedia: () => void;
    stopMedia: () => void;
}

export class MediaRecorderAPIService implements IMediaRecorderService {

    private audioConstraints: boolean = true;
    private videoConstraints: boolean = false;
    private audioFormat: string = "audio/ogg; codecs=opus";
    private videoFormat: string = "video/mp4";
    private stopCallBack: Function = Function();
    private mediaRecorder?: MediaRecorder;

    private mediaChunks: Blob[] = [];

    constructor(audioConstraints?: boolean, videoConstraints?: boolean, audioFormat?: string, videoFormat?: string, stopCallBack?: Function) {
        this.audioConstraints = audioConstraints ?? this.audioConstraints;
        this.videoConstraints = videoConstraints ?? this.videoConstraints;
        this.audioFormat = audioFormat ?? this.audioFormat;
        this.videoFormat = videoFormat ?? this.videoFormat;
        this.stopCallBack = stopCallBack ?? this.stopCallBack;
    }


    public pauseMedia() {
        this.mediaRecorder?.pause();
    }
    public resumeMedia() {
        this.mediaRecorder?.resume();

    };
    public async startMedia() {
        await this.initializeAndStartMediaRecorder();
        this.mediaRecorder?.start();
    };
    public stopMedia() {
        this.mediaRecorder?.stop();
    };

    private async initializeAndStartMediaRecorder() {

        const stream = await navigator.mediaDevices.getUserMedia({ audio: this.audioConstraints });

        this.mediaRecorder = new MediaRecorder(stream);
        this.mediaRecorder.ondataavailable = (e) => {

            this.mediaChunks.push(e.data);
        };

        this.mediaRecorder.onstop = (_) => {
            const b: Blob = this.createMediaBlob();
            this.stopCallBack(b);
            this.cleanMediaStream();
        };


    }

    private createMediaBlob(): Blob {
        const blob = new Blob(
            this.mediaChunks, {
            'type': this.videoConstraints ?
                this.videoFormat : this.audioFormat,

        });
        return blob;
    }


    private cleanMediaStream() {
        this.mediaChunks = [];
        this.closeMediaRecorderStream();
    }

    private closeMediaRecorderStream() {
        this.mediaRecorder?.stream.getTracks()
            .forEach(track => track.stop());
    }

}