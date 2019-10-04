#ifndef LWSPEEDOMETER_SEQUENCEGENERATOR_H
#define LWSPEEDOMETER_SEQUENCEGENERATOR_H

#include <cstdint>
#include "jni.h"

class SequenceGenerator {
private:
    float current_value = 0;

    static constexpr float GENERATION_STEP = 10;
    static const int GENERATION_SIN_KOEFF1 = 500;
    static const int GENERATION_SIN_KOEFF2 = 300;

public:
    float max_value = 0;

    float next_value();
};

#endif //LWSPEEDOMETER_SEQUENCEGENERATOR_H
