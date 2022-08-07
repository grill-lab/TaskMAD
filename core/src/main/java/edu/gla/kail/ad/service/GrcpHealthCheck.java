package edu.gla.kail.ad.service;

import grpc.health.v1.GrpcHealthCheck.HealthCheckRequest;
import grpc.health.v1.GrpcHealthCheck.HealthCheckResponse;
import grpc.health.v1.HealthGrpc.HealthImplBase;
import io.grpc.stub.StreamObserver;

public class GrcpHealthCheck extends HealthImplBase{
    private static boolean isServerHealthy = true;

    @Override
    public void check(HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
        if (isServerHealthy){
            responseObserver.onNext(
                HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.SERVING).build());   
        }else{
            responseObserver.onNext(
                HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.NOT_SERVING).build());   
        }

        responseObserver.onCompleted();
    }

    public void setServerUnhealthy(){
        isServerHealthy = false;
    }

    public void setServerHealthy(){
        isServerHealthy = true;
    }

    
}
