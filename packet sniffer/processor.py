# Modified from https://www.vipinajayakumar.com/parsing-text-with-python/

import re # regex
from sphero_constants import *

# https://regexper.com/

rx_dict = {
    # Have different elements for different commands...
    
    'data_byte_s': re.compile(r'A\t.*\(0x\) (?P<data_byte_s>([0-9A-F]{2}-*)+).*received\n')
}
pattern_byte = r'(?P<data_byte_s>[0-9A-F]{2})'


def _parse_line(line):
    """
    Do a regex search against all defined regexes and
    return the key and match result of the first matching regex

    """

    for key, rx in rx_dict.items():
        match = rx.search(line)
        if match:
            return key, match
    # if there are no matches
    return None, None
    
    
    
def parse_file(filepath):
    """
    Parse text at given filepath

    Parameters
    ----------
    filepath : str
        Filepath for file_object to be parsed

    Returns
    -------
    data : pd.DataFrame
        Parsed data

    """

    data = []  # create an empty list to collect the data
    # open the file and read through it line by line
    with open(filepath, 'r') as file_object:
        line = file_object.readline()
        while line:
            # at each line check for a match with a regex
            key, match = _parse_line(line)

            # extract data bytes
            if key == 'data_byte_s':
                data_byte_s = match.group('data_byte_s')
                data_byte_s = re.findall(pattern_byte, data_byte_s)
                for byte in data_byte_s:
                    data.append(int(byte, 16))

            line = file_object.readline()


    return data
    
    
# From https://github.com/MProx/Sphero_mini
def bits_to_num(bits):
    '''
    This helper function decodes bytes from sensor packets into single precision floats. Encoding follows the
    the IEEE-754 standard.
    '''
    num = int(bits, 2).to_bytes(len(bits) // 8, byteorder='little')
    num = struct.unpack('f', num)[0]
    return num
    

notificationPacket = []
notification_ack = ""
notification_seq = 0
# From https://github.com/MProx/Sphero_mini
def handleNotification(data):
    global notificationPacket, notification_ack, notification_seq
    '''
    The method keeps appending bytes to the payload packet byte list until end-of-packet byte is
    encountered. Note that this is an issue, because 0xD8 could be sent as part of the payload of,
    say, the battery voltage notification. In future, a more sophisticated method will be required.
    '''
    for data_byte in data: # parse each byte separately (sometimes they arrive simultaneously)

        notificationPacket.append(data_byte) # Add new byte to packet list
        # If end of packet (need to find a better way to segment the packets):
        if data_byte == sendPacketConstants['EndOfPacket']:
            for num in notificationPacket:
                print("0x%0.2X" % num + " ", end = '')
            print('')
            # Once full the packet has arrived, parse it:
            # Packet structure is similar to the outgoing send packets (see docstring in sphero_mini._send())
            
            # Attempt to unpack. Might fail if packet is too badly corrupted
            try:
                start, flags_bits, devid, commcode, seq, *notification_payload, chsum, end = notificationPacket
            except ValueError:
                print("Warning: notification packet unparseable", notificationPacket)
                notificationPacket = [] # Discard this packet
                continue # exit

            # Compute and append checksum and add EOP byte:
            # From Sphero docs: "The [checksum is the] modulo 256 sum of all the bytes
            #                   from the device ID through the end of the data payload,
            #                   bit inverted (1's complement)"
            # For the sphero mini, the flag bits must be included too:
            checksum_bytes = [flags_bits, devid, commcode, seq] + notification_payload
            checksum = 0 # init
            for num in checksum_bytes:
                checksum = (checksum + num) & 0xFF # bitwise "and to get modulo 256 sum of appropriate bytes
            checksum = 0xff - checksum # bitwise 'not' to invert checksum bits
            if checksum != chsum: # check computed checksum against that recieved in the packet
                print("Warning: notification packet checksum failed", notificationPacket)
                notificationPacket = [] # Discard this packet
                continue # exit

            # Check if response packet:
            if flags_bits & flags['isResponse']: # it is a response

                # Use device ID and command code to determine which command is being acknowledged:
                if devid == deviceID['powerInfo'] and commcode == powerCommandIDs['wake']:
                    notification_ack = "Wake acknowledged" # Acknowledgement after wake command
                    
                elif devid == deviceID['driving'] and commcode == drivingCommands['driveWithHeading']:
                    notification_ack = "Roll command acknowledged"

                elif devid == deviceID['driving'] and commcode == drivingCommands['stabilization']:
                    notification_ack = "Stabilization command acknowledged"

                elif devid == deviceID['userIO'] and commcode == userIOCommandIDs['allLEDs']:
                    notification_ack = "LED/backlight color command acknowledged"

                elif devid == deviceID['driving'] and commcode == drivingCommands["resetHeading"]:
                    notification_ack = "Heading reset command acknowledged"

                elif devid == deviceID['sensor'] and commcode == sensorCommands["configureCollision"]:
                    notification_ack = "Collision detection configuration acknowledged"

                elif devid == deviceID['sensor'] and commcode == sensorCommands["configureSensorStream"]:
                    notification_ack = "Sensor stream configuration acknowledged"

                elif devid == deviceID['sensor'] and commcode == sensorCommands["sensorMask"]:
                    notification_ack = "Mask configuration acknowledged"

                elif devid == deviceID['sensor'] and commcode == sensorCommands["sensor1"]:
                    notification_ack = "Sensor1 acknowledged"

                elif devid == deviceID['sensor'] and commcode == sensorCommands["sensor2"]:
                    notification_ack = "Sensor2 acknowledged"

                elif devid == deviceID['powerInfo'] and commcode == powerCommandIDs['batteryVoltage']:
                    V_batt = notification_payload[2] + notification_payload[1]*256 + notification_payload[0]*65536
                    V_batt /= 100 # Notification gives V_batt in 10mV increments. Divide by 100 to get to volts.
                    notification_ack = "Battery voltage:" + str(V_batt) + "v"

                elif devid == deviceID['systemInfo'] and commcode == SystemInfoCommands['mainApplicationVersion']:
                    version = '.'.join(str(x) for x in notification_payload)
                    notification_ack = "Firmware version: " + version
                                            
                else:
                    notification_ack = "Unknown acknowledgement" #print(notificationPacket)
                    print("===================> Unknown ack packet")

                notification_seq = seq
                print(notification_ack)

            else: # Not a response packet - therefore, asynchronous notification (e.g. collision detection, etc):
                
                # Collision detection:
                if devid == deviceID['sensor'] and commcode == sensorCommands['collisionDetectedAsync']:
                    # The first four bytes are data that is still un-parsed. the remaining unsaved bytes are always zeros
                    _, _, _, _, _, _, axis, _, Y_mag, _, X_mag, *_ = notification_payload
                    if axis == 1: 
                        dir = "Left/right"
                    else:
                        dir = 'Forward/back'
                    print("Collision detected:")
                    print("\tAxis:", dir)
                    print("\tX_mag:", X_mag)
                    print("\tY_mag:", Y_mag)

                # Sensor response:
                elif devid == deviceID['sensor'] and commcode == sensorCommands['sensorResponse']:
                    # Convert to binary, pad bytes with leading zeros:
                    val = ''
                    for byte in notification_payload:
                        val += format(int(bin(byte)[2:], 2), '#010b')[2:]
                    
                    # Break into 32-bit chunks
                    nums = []
                    while(len(val) > 0):
                        num, val = val[:32], val[32:] # Slice off first 16 bits
                        nums.append(num)
                    
                    # convert from raw bits to float:
                    nums = [bits_to_num(num) for num in nums]

                    # Set sensor values as class attributes:
                    print("Sensor value attributes: " + nums)
                    
                # Unrecognized packet structure:
                else:
                    notification_ack = "Unknown asynchronous notification" #print(notificationPacket)
                    print(notification_ack)
                    #print(notificationPacket, "===================> Unknown async packet")
                    
            notificationPacket = [] # Start new payload after this byte
            notification_ack = ""
            print('')




    
if __name__ == '__main__':
    #filepath = 'Log connecting from my app.txt'
    #filepath = 'Log connecting from the app after struggling to connect.txt'
    #filepath = 'Log heading LED.txt'
    filepath = 'Log heading LED mine.txt'
    #filepath = 'fake log.txt'
    #filepath = 'Log reconnecting to their app after switching open app.txt'
    data = parse_file(filepath)
    #print(data)
    handleNotification(data)