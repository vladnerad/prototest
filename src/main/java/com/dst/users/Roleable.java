package com.dst.users;

import com.dst.msg.WarehouseMessage;

interface Roleable {
    WarehouseMessage.LogInResponse.Role getRole();
}
