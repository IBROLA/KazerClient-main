void main() {
    vec4 color=SAMPLE(DiffuseSampler,uv);
    float angle=radians(Hue*360.0);
    vec3 k=vec3(0.57735);
    color.rgb=color.rgb*cos(angle)+cross(k,color.rgb)*sin(angle)+k*dot(k,color.rgb)*(1.0-cos(angle));
    color.rgb=(color.rgb-0.5)*(Contrast*2.0)+0.5+vec3(Brightness*2.0-1.0);
    float luma=sqrt(dot(color.rgb*color.rgb,vec3(0.299,0.587,0.114)));
    color.rgb=vec3(luma)+(color.rgb-vec3(luma))*(Saturation*2.0);
    OUT_COLOR=color;
}
